package com.dove.scheduler.service;

import com.dove.krx.DailyMarketData;
import com.dove.krx.infrastructure.adapter.KrxTradingDayFetcher;
import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.dto.ScanResult;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataCompletionServiceTest {

    @Mock MarketTradingDateService marketTradingDateService;
    @Mock DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock KrxTradingDayFetcher krxTradingDayFetcher;
    @Mock MarketDayWriter marketDayWriter;

    MarketDataCompletionService service;

    static final MarketType MARKET = MarketType.KOSPI;
    // 2026-01-05(월) ~ 2026-01-09(금) : 평일 5일
    static final LocalDate FROM  = LocalDate.of(2026, 1, 5);
    static final LocalDate UP_TO = LocalDate.of(2026, 1, 9);

    @BeforeEach
    void setUp() {
        service = new MarketDataCompletionService(
                marketTradingDateService, dailyStockPriceQueryService,
                krxTradingDayFetcher, marketDayWriter);
    }

    // ── syncRange ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("syncRange → fetch 호출 후 각 day를 write, OPEN 날짜 반환")
    void shouldFetchAndWriteAllDaysInRange() {
        LocalDate d1 = LocalDate.of(2026, 1, 5);
        LocalDate d2 = LocalDate.of(2026, 1, 6);
        when(krxTradingDayFetcher.fetch(MARKET, d1, d2)).thenReturn(List.of(
                DailyMarketData.open(d1, List.of(), List.of()),
                DailyMarketData.closed(d2)
        ));

        List<LocalDate> openDates = service.syncRange(MARKET, d1, d2);

        verify(krxTradingDayFetcher).fetch(MARKET, d1, d2);
        verify(marketDayWriter, times(2)).write(eq(MARKET), any());
        assertThat(openDates).containsExactly(d1);
    }

    @Test
    @DisplayName("syncRange — write 실패한 날짜는 반환 목록에서 제외, 나머지 계속 처리")
    void shouldSkipFailedDayAndContinueInSyncRange() {
        LocalDate d1 = LocalDate.of(2026, 1, 5);
        LocalDate d2 = LocalDate.of(2026, 1, 6);
        when(krxTradingDayFetcher.fetch(MARKET, d1, d2)).thenReturn(List.of(
                DailyMarketData.open(d1, List.of(), List.of()),
                DailyMarketData.open(d2, List.of(), List.of())
        ));
        doThrow(new RuntimeException("DB 오류"))
                .when(marketDayWriter).write(eq(MARKET), argThat(day -> day.date().equals(d1)));

        List<LocalDate> openDates = service.syncRange(MARKET, d1, d2);

        assertThat(openDates).containsExactly(d2);
        verify(marketDayWriter, times(2)).write(eq(MARKET), any());
    }

    // ── scan ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("빈 DB → 평일 전체 범위로 fetch 호출")
    void shouldFetchFullWeekdayRangeWhenDbIsEmpty() {
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Set.of());
        when(marketTradingDateService.findStatusesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Map.of());
        when(krxTradingDayFetcher.fetch(MARKET, FROM, UP_TO)).thenReturn(List.of());

        ScanResult result = service.scan(MARKET, FROM, UP_TO, 100);

        verify(krxTradingDayFetcher).fetch(eq(MARKET), eq(FROM), eq(UP_TO));
        assertThat(result.total()).isEqualTo(5);
    }

    @Test
    @DisplayName("모든 평일에 주가 존재 → fetch 없음, 스킵 결과")
    void shouldSkipWhenAllWeekdaysHavePrices() {
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Set.of(
                        LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6),
                        LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 8),
                        LocalDate.of(2026, 1, 9)));
        when(marketTradingDateService.findStatusesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Map.of());

        ScanResult result = service.scan(MARKET, FROM, UP_TO, 100);

        verify(krxTradingDayFetcher, never()).fetch(any(), any(), any());
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("MTD CLOSED 확정 날짜 → fetch 범위에서 제외")
    void shouldExcludeConfirmedClosedDates() {
        LocalDate mon = LocalDate.of(2026, 1, 5);
        LocalDate tue = LocalDate.of(2026, 1, 6);
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Set.of(
                        LocalDate.of(2026, 1, 7),
                        LocalDate.of(2026, 1, 8),
                        LocalDate.of(2026, 1, 9)));
        when(marketTradingDateService.findStatusesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(mon, false, tue, false));

        ScanResult result = service.scan(MARKET, FROM, UP_TO, 100);

        verify(krxTradingDayFetcher, never()).fetch(any(), any(), any());
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("일부 평일 누락 → 누락 최솟값~최댓값 범위로 fetch 호출")
    void shouldFetchMissingRangeWhenSomeDatesAbsent() {
        LocalDate missing = LocalDate.of(2026, 1, 7);
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Set.of(
                        LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6),
                        LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9)));
        when(marketTradingDateService.findStatusesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Map.of());
        when(krxTradingDayFetcher.fetch(MARKET, missing, missing))
                .thenReturn(List.of(DailyMarketData.open(missing, List.of(), List.of())));

        ScanResult result = service.scan(MARKET, FROM, UP_TO, 100);

        verify(krxTradingDayFetcher).fetch(eq(MARKET), eq(missing), eq(missing));
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.succeeded()).isEqualTo(1);
    }

    @Test
    @DisplayName("누락 수 > maxDates → 가장 오래된 날짜부터 maxDates건만 fetch")
    void shouldLimitFetchToMaxDatesFromOldest() {
        LocalDate mon = LocalDate.of(2026, 1, 5);
        LocalDate wed = LocalDate.of(2026, 1, 7);
        // 월~금 5일 모두 누락, maxDates=3 → 월~수(3일)만 처리
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Set.of());
        when(marketTradingDateService.findStatusesInRange(MARKET, FROM, UP_TO))
                .thenReturn(Map.of());
        when(krxTradingDayFetcher.fetch(MARKET, mon, wed)).thenReturn(List.of());

        ScanResult result = service.scan(MARKET, FROM, UP_TO, 3);

        verify(krxTradingDayFetcher).fetch(eq(MARKET), eq(mon), eq(wed));
        assertThat(result.total()).isEqualTo(3);
    }
}
