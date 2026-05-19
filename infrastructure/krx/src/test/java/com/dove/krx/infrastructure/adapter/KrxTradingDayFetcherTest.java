package com.dove.krx.infrastructure.adapter;

import com.dove.krx.DailyMarketData;
import com.dove.krx.DayStatus;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrxTradingDayFetcherTest {

    @Mock KrxStockClient krxStockClient;

    KrxTradingDayFetcher fetcher;

    static final String AUTH_KEY = "test-key";
    static final MarketType MARKET = MarketType.KOSPI;
    static final LocalDate DATE = LocalDate.of(2026, 4, 17);

    @BeforeEach
    void setUp() {
        fetcher = new KrxTradingDayFetcher(krxStockClient);
        ReflectionTestUtils.setField(fetcher, "krxApiAuthKey", AUTH_KEY);
    }

    // ── 팩토리 ────────────────────────────────────────────────────────────────

    private KrxListedStockResponse listingOf(String... tickers) {
        List<KrxListedStockResponse.Item> items = Arrays.stream(tickers)
                .map(t -> new KrxListedStockResponse.Item(t, "KR7" + t + "003", "종목" + t, "19750611"))
                .toList();
        return new KrxListedStockResponse(items);
    }

    private KrxListedStockResponse emptyListing() {
        return new KrxListedStockResponse(List.of());
    }

    private KrxDailyPriceResponse priceOf(LocalDate date, String... codes) {
        List<KrxDailyPriceResponse.Data> data = Arrays.stream(codes)
                .map(code -> new KrxDailyPriceResponse.Data(
                        date.format(DateTimeFormatter.BASIC_ISO_DATE), code, "종목" + code,
                        "KOSPI", "-", "60000", "0", "0", "59000", "62000", "59000",
                        "1000", "60000000", "6000000000", "100000000"))
                .toList();
        return new KrxDailyPriceResponse(data);
    }

    private KrxDailyPriceResponse emptyPrice() {
        return new KrxDailyPriceResponse(List.of());
    }

    private FeignException.Unauthorized buildUnauthorized() {
        return new FeignException.Unauthorized("unauthorized",
                Request.create(Request.HttpMethod.GET, "/test",
                        Collections.emptyMap(), Request.Body.empty(), new RequestTemplate()),
                new byte[0], Collections.emptyMap());
    }

    private FeignException.ServiceUnavailable buildServiceUnavailable() {
        return new FeignException.ServiceUnavailable("unavailable",
                Request.create(Request.HttpMethod.GET, "/test",
                        Collections.emptyMap(), Request.Body.empty(), new RequestTemplate()),
                new byte[0], Collections.emptyMap());
    }

    // ── OPEN ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("종목·주가 모두 있음 → OPEN 반환")
    void shouldReturnOpenWhenListingsAndPricesPresent() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenReturn(priceOf(DATE, "005930"));

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, DATE);

        assertThat(result).hasSize(1);
        DailyMarketData day = result.get(0);
        assertThat(day.status()).isEqualTo(DayStatus.OPEN);
        assertThat(day.date()).isEqualTo(DATE);
        assertThat(day.listings()).hasSize(1);
        assertThat(day.prices()).hasSize(1);
    }

    // ── UNCONFIRMED (즉시 확정) ────────────────────────────────────────────────

    @Test
    @DisplayName("종목·주가 모두 빈 응답 → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakWhenBothEmpty() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(emptyListing());
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenReturn(emptyPrice());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        // 병렬 fetch 구조상 nextDate API 호출은 발생하나, 결과 리스트는 UNCONFIRMED 1건에서 중단
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("종목 조회 Unauthorized → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakOnListingUnauthorized() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenThrow(buildUnauthorized());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("종목 조회 FeignException → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakOnListingFeignException() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenThrow(buildServiceUnavailable());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("주가 조회 Unauthorized → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakOnPriceUnauthorized() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenThrow(buildUnauthorized());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("주가 조회 FeignException → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakOnPriceFeignException() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenThrow(buildServiceUnavailable());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("주가 응답 null → 결과에 UNCONFIRMED 1건만 포함")
    void shouldReturnUnconfirmedAndBreakOnNullPriceResponse() {
        LocalDate nextDate = DATE.plusDays(1);
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenReturn(null);

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, nextDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    // ── NO_PRICE 후처리 (CLOSED / UNCONFIRMED 결정) ───────────────────────────

    @Test
    @DisplayName("종목 있음·주가 없음, 구간 내 OPEN 없음 → UNCONFIRMED")
    void shouldReturnUnconfirmedWhenNoPriceAndNoOpenInRange() {
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, DATE)).thenReturn(emptyPrice());

        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE, DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("lastOpenDate 이전 NO_PRICE → CLOSED, 이후 NO_PRICE → UNCONFIRMED")
    void shouldResolveNoPriceBasedOnLastOpenDate() {
        LocalDate d1 = DATE;                // NO_PRICE (주가 없음)
        LocalDate d2 = DATE.plusDays(1);    // OPEN
        LocalDate d3 = DATE.plusDays(2);    // NO_PRICE (주가 없음)

        when(krxStockClient.getKospiListedStocks(AUTH_KEY, d1)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, d1)).thenReturn(emptyPrice());
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, d2)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, d2)).thenReturn(priceOf(d2, "005930"));
        when(krxStockClient.getKospiListedStocks(AUTH_KEY, d3)).thenReturn(listingOf("005930"));
        when(krxStockClient.getDailyKospiStockInfo(AUTH_KEY, d3)).thenReturn(emptyPrice());

        List<DailyMarketData> result = fetcher.fetch(MARKET, d1, d3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.CLOSED);        // d1: lastOpenDate(d2) 이전
        assertThat(result.get(1).status()).isEqualTo(DayStatus.OPEN);           // d2
        assertThat(result.get(2).status()).isEqualTo(DayStatus.UNCONFIRMED);    // d3: lastOpenDate(d2) 이후
    }

    // ── 시장별 엔드포인트 분기 ────────────────────────────────────────────────

    @Test
    @DisplayName("KOSDAQ 시장 → KOSDAQ 엔드포인트 호출")
    void shouldCallKosdaqEndpoint() {
        when(krxStockClient.getKosdaqListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("035720"));
        when(krxStockClient.getDailyKosdaqStockInfo(AUTH_KEY, DATE)).thenReturn(priceOf(DATE, "035720"));

        List<DailyMarketData> result = fetcher.fetch(MarketType.KOSDAQ, DATE, DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.OPEN);
        verify(krxStockClient).getKosdaqListedStocks(AUTH_KEY, DATE);
        verify(krxStockClient, never()).getKospiListedStocks(any(), any());
    }

    @Test
    @DisplayName("KONEX 시장 → KONEX 엔드포인트 호출")
    void shouldCallKonexEndpoint() {
        when(krxStockClient.getKonexListedStocks(AUTH_KEY, DATE)).thenReturn(listingOf("900140"));
        when(krxStockClient.getDailyKonexStockInfo(AUTH_KEY, DATE)).thenReturn(priceOf(DATE, "900140"));

        List<DailyMarketData> result = fetcher.fetch(MarketType.KONEX, DATE, DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DayStatus.OPEN);
        verify(krxStockClient).getKonexListedStocks(AUTH_KEY, DATE);
        verify(krxStockClient, never()).getKospiListedStocks(any(), any());
    }

    // ── 빈 구간 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("from > to → 빈 리스트 반환")
    void shouldReturnEmptyListWhenFromAfterTo() {
        List<DailyMarketData> result = fetcher.fetch(MARKET, DATE.plusDays(1), DATE);

        assertThat(result).isEmpty();
        verifyNoInteractions(krxStockClient);
    }
}
