package com.dove.krx.infrastructure.adapter;

import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.market.domain.enums.MarketType;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KrxStockListingFetcherTest {

    @Mock
    private KrxStockClient krxStockClient;

    private KrxStockListingFetcher fetcher;

    private final LocalDate date = LocalDate.of(2026, 4, 17);
    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private static final String AUTH_KEY = "test-key";

    @BeforeEach
    void setUp() {
        fetcher = new KrxStockListingFetcher(krxStockClient, clock);
        ReflectionTestUtils.setField(fetcher, "krxApiAuthKey", AUTH_KEY);
    }

    private Request buildRequest() {
        return Request.create(Request.HttpMethod.GET, "url",
                Map.of(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
    }

    @Test
    @DisplayName("KOSPI 정상 응답 → Success(Map)")
    void shouldReturnSuccessWhenKospiDataPresent() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date)).thenReturn(
                new KrxListedStockResponse(List.of(
                        new KrxListedStockResponse.Item("KR7005930003", "삼성전자"),
                        new KrxListedStockResponse.Item("KR7000660001", "SK하이닉스")
                ))
        );

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.Success.class);
        Map<String, String> stocks = ((StockListingFetcher.Outcome.Success) outcome).stocks();
        assertThat(stocks).containsEntry("KR7005930003", "삼성전자")
                .containsEntry("KR7000660001", "SK하이닉스");
    }

    @Test
    @DisplayName("KOSDAQ 엔드포인트 분기")
    void shouldFetchKosdaq() {
        when(krxStockClient.getKosdaqListedStocks(AUTH_KEY, date)).thenReturn(
                new KrxListedStockResponse(List.of(
                        new KrxListedStockResponse.Item("KR7035720002", "카카오")
                ))
        );

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSDAQ, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.Success.class);
        assertThat(((StockListingFetcher.Outcome.Success) outcome).stocks())
                .containsEntry("KR7035720002", "카카오");
    }

    @Test
    @DisplayName("KONEX 엔드포인트 분기")
    void shouldFetchKonex() {
        when(krxStockClient.getKonexListedStocks(AUTH_KEY, date)).thenReturn(
                new KrxListedStockResponse(List.of(
                        new KrxListedStockResponse.Item("KR7900140009", "KTcs")
                ))
        );

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KONEX, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.Success.class);
    }

    @Test
    @DisplayName("응답 null → PermanentFail PARSE_FAILED")
    void shouldReturnPermanentFailOnNullResponse() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date)).thenReturn(null);

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.PermanentFail.class);
        assertThat(((StockListingFetcher.Outcome.PermanentFail) outcome).reason())
                .isEqualTo(StockListingFetcher.Reason.PARSE_FAILED);
    }

    @Test
    @DisplayName("OutBlock_1 빈 배열 → Holiday")
    void shouldReturnHolidayOnEmptyItems() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date))
                .thenReturn(new KrxListedStockResponse(List.of()));

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.Holiday.class);
    }

    @Test
    @DisplayName("동일 ISU_CD 중복 → Success에서 첫 항목 유지")
    void shouldKeepFirstOnDuplicateKeys() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date)).thenReturn(
                new KrxListedStockResponse(List.of(
                        new KrxListedStockResponse.Item("KR7005930003", "삼성전자"),
                        new KrxListedStockResponse.Item("KR7005930003", "삼성전자우")
                ))
        );

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.Success.class);
        assertThat(((StockListingFetcher.Outcome.Success) outcome).stocks())
                .containsEntry("KR7005930003", "삼성전자");
    }

    @Test
    @DisplayName("FeignException.Unauthorized → PermanentFail AUTH_FAILED")
    void shouldReturnPermanentFailOnUnauthorized() {
        FeignException.Unauthorized unauthorized = new FeignException.Unauthorized(
                "unauthorized", buildRequest(), null, Map.of());
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date)).thenThrow(unauthorized);

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.PermanentFail.class);
        assertThat(((StockListingFetcher.Outcome.PermanentFail) outcome).reason())
                .isEqualTo(StockListingFetcher.Reason.AUTH_FAILED);
    }

    @Test
    @DisplayName("일반 FeignException → RetryLater TRANSIENT with now+300s")
    void shouldReturnRetryLaterOnFeignException() {
        FeignException.ServiceUnavailable serviceUnavailable = new FeignException.ServiceUnavailable(
                "unavailable",
                Request.create(Request.HttpMethod.GET, "/test",
                        Collections.emptyMap(), Request.Body.empty(), new RequestTemplate()),
                new byte[0], Collections.emptyMap());
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, date)).thenThrow(serviceUnavailable);

        StockListingFetcher.Outcome outcome = fetcher.fetch(MarketType.KOSPI, date);

        assertThat(outcome).isInstanceOf(StockListingFetcher.Outcome.RetryLater.class);
        StockListingFetcher.Outcome.RetryLater rl = (StockListingFetcher.Outcome.RetryLater) outcome;
        assertThat(rl.reason()).isEqualTo(StockListingFetcher.Reason.TRANSIENT);
        assertThat(rl.nextRetryAt()).isEqualTo(now.plusSeconds(300));
    }
}
