package com.dove.krx.infrastructure.adapter;

import com.dove.stockcollection.application.port.StockListing;
import com.dove.krx.infrastructure.client.KrxListedStockItem;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.krx.quota.KrxAccessBlockedException;
import com.dove.krx.quota.KrxApiQuotaService;
import com.dove.krx.quota.KrxDailyQuotaExceededException;
import com.dove.krx.quota.KrxRemoteRateLimitException;
import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.application.service.SystemEventService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KrxTradingDayAdapter")
class KrxTradingDayAdapterTest {

    @Mock KrxStockClient krxStockClient;
    @Mock KrxApiQuotaService quotaService;
    @Mock SystemEventService systemEventService;

    KrxTradingDayAdapter adapter;

    static final String AUTH_KEY = "test-key";
    static final MarketType MARKET = MarketType.KOSPI;
    static final LocalDate DATE = LocalDate.of(2026, 4, 17);

    @BeforeEach
    void setUp() {
        adapter = new KrxTradingDayAdapter(krxStockClient, Optional.of(quotaService), systemEventService);
        ReflectionTestUtils.setField(adapter, "krxApiAuthKey", AUTH_KEY);
    }

    private KrxListedStockResponse listingOf(String... tickers) {
        List<KrxListedStockItem> items = Arrays.stream(tickers)
                .map(t -> new KrxListedStockItem(
                        t, "KR7" + t + "003", "종목" + t, null, null,
                        "19750611", null, "주권", null, "보통주", null, null))
                .toList();
        return new KrxListedStockResponse(items);
    }

    private FeignException feignException(Class<? extends FeignException> type) {
        Request request = Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), Request.Body.empty(), new RequestTemplate());
        try {
            return type.getConstructor(String.class, Request.class, byte[].class, java.util.Map.class)
                    .newInstance("error", request, new byte[0], Collections.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("fetchListings")
    class FetchListings {

        @Test
        @DisplayName("shouldReturnListingsWhenResponseContainsItems")
        void shouldReturnListingsWhenResponseContainsItems() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));

            List<StockListing> result = adapter.fetchListings(MARKET, DATE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ticker()).isEqualTo("005930");
        }

        @Test
        @DisplayName("shouldReturnEmptyWhenResponseItemsIsEmpty")
        void shouldReturnEmptyWhenResponseItemsIsEmpty() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenReturn(new KrxListedStockResponse(List.of()));

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
        }

        @Test
        @DisplayName("shouldReturnEmptyWhenResponseIsNull")
        void shouldReturnEmptyWhenResponseIsNull() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(null);

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
        }

        @Test
        @DisplayName("shouldReturnEmptyAndMarkRateLimitWhenDailyQuotaExceeded")
        void shouldReturnEmptyAndMarkRateLimitWhenDailyQuotaExceeded() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenThrow(new KrxDailyQuotaExceededException());

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
            verify(quotaService).markRemoteRateLimited();
        }

        @Test
        @DisplayName("shouldReturnEmptyAndMarkRateLimitWhenRemoteRateLimited")
        void shouldReturnEmptyAndMarkRateLimitWhenRemoteRateLimited() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenThrow(new KrxRemoteRateLimitException("rate limit"));

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
            verify(quotaService).markRemoteRateLimited();
            verify(systemEventService).recordKrxRateLimit(MARKET, DATE, "rate limit");
        }

        @Test
        @DisplayName("shouldReturnEmptyAndRecordFailureWhenUnauthorized")
        void shouldReturnEmptyAndRecordFailureWhenUnauthorized() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenThrow(feignException(FeignException.Unauthorized.class));

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
            verifyNoInteractions(quotaService);
            verify(systemEventService).recordKrxApiFailure(MARKET, "KRX 인증 오류: " + DATE);
        }

        @Test
        @DisplayName("shouldThrowKrxAccessBlockedWhenForbidden")
        void shouldThrowKrxAccessBlockedWhenForbidden() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenThrow(feignException(FeignException.Forbidden.class));

            assertThatThrownBy(() -> adapter.fetchListings(MARKET, DATE))
                    .isInstanceOf(KrxAccessBlockedException.class);
            verify(systemEventService).recordKrxApiFailure(MARKET, "KRX 403 Access Denied: " + DATE);
        }

        @Test
        @DisplayName("shouldReturnEmptyWhenFeignException")
        void shouldReturnEmptyWhenFeignException() {
            when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE))
                    .thenThrow(feignException(FeignException.ServiceUnavailable.class));

            assertThat(adapter.fetchListings(MARKET, DATE)).isEmpty();
            verifyNoInteractions(quotaService);
        }
    }

    @Nested
    @DisplayName("시장별 엔드포인트 라우팅")
    class MarketRouting {

        @Test
        @DisplayName("shouldCallKosdaqEndpointWhenMarketIsKosdaq")
        void shouldCallKosdaqEndpointWhenMarketIsKosdaq() {
            when(krxStockClient.getKosdaqListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("035720"));

            List<StockListing> result = adapter.fetchListings(MarketType.KOSDAQ, DATE);

            assertThat(result).hasSize(1);
            verify(krxStockClient).getKosdaqListedStocks(AUTH_KEY, DATE);
            verify(krxStockClient, never()).getKospiListedStocks(any(), any());
        }

        @Test
        @DisplayName("shouldCallKonexEndpointWhenMarketIsKonex")
        void shouldCallKonexEndpointWhenMarketIsKonex() {
            when(krxStockClient.getKonexListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("900140"));

            List<StockListing> result = adapter.fetchListings(MarketType.KONEX, DATE);

            assertThat(result).hasSize(1);
            verify(krxStockClient).getKonexListedStocks(AUTH_KEY, DATE);
            verify(krxStockClient, never()).getKospiListedStocks(any(), any());
        }
    }
}
