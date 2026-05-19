package com.dove.scheduler.job;

import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.service.IndicatorDailyCalculateService;
import com.dove.scheduler.service.MarketDataCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMarketJobTest {

    @Mock MarketDataCompletionService marketDataCompletionService;
    @Mock IndicatorDailyCalculateService indicatorDailyCalculateService;
    @Mock MarketTradingDateService marketTradingDateService;

    DailyMarketJob job;

    static final LocalDate TODAY = LocalDate.of(2026, 4, 22);
    /** T+1: upTo = 전일 */
    static final LocalDate UP_TO = TODAY.minusDays(1); // 2026-04-21

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        job = new DailyMarketJob(
                marketDataCompletionService, indicatorDailyCalculateService,
                marketTradingDateService, fixedClock, List.of(MarketType.KOSPI), new ForkJoinPool(2));
    }

    @Test
    @DisplayName("커서 없음 → 오늘 기준 1년 전부터 upTo까지 syncRange 호출")
    void shouldCallSyncFromOneYearAgoWhenNoCursor() {
        when(marketTradingDateService.findLastProcessedDate(MarketType.KOSPI)).thenReturn(Optional.empty());
        when(marketDataCompletionService.syncRange(any(), any(), any())).thenReturn(List.of());

        job.run();

        verify(marketDataCompletionService)
                .syncRange(eq(MarketType.KOSPI), eq(TODAY.minusYears(1)), eq(UP_TO));
    }

    @Test
    @DisplayName("커서가 upTo와 같음 → syncRange 호출 없음")
    void shouldSkipSyncWhenCursorEqualsUpTo() {
        when(marketTradingDateService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(UP_TO));

        job.run();

        verify(marketDataCompletionService, never()).syncRange(any(), any(), any());
    }

    @Test
    @DisplayName("커서가 upTo 이후 → syncRange 호출 없음")
    void shouldSkipSyncWhenCursorIsAfterUpTo() {
        when(marketTradingDateService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(UP_TO.plusDays(1)));

        job.run();

        verify(marketDataCompletionService, never()).syncRange(any(), any(), any());
    }

    @Test
    @DisplayName("커서가 upTo 하루 전 → cursor+1(=upTo)부터 upTo까지 syncRange 호출")
    void shouldCallSyncFromCursorPlusOneWhenCursorExistsBeforeUpTo() {
        LocalDate cursorDate = UP_TO.minusDays(1);
        when(marketTradingDateService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(cursorDate));
        when(marketDataCompletionService.syncRange(any(), any(), any())).thenReturn(List.of());

        job.run();

        verify(marketDataCompletionService).syncRange(MarketType.KOSPI, UP_TO, UP_TO);
    }

    @Test
    @DisplayName("syncRange로 OPEN 날짜 반환 시 각 날짜별 calculateAndSave 호출")
    void shouldCalculateIndicatorsForEachOpenDate() {
        LocalDate d1 = UP_TO.minusDays(1);
        LocalDate d2 = UP_TO;
        when(marketTradingDateService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(d1.minusDays(1)));
        when(marketDataCompletionService.syncRange(any(), any(), any())).thenReturn(List.of(d1, d2));

        job.run();

        verify(indicatorDailyCalculateService).calculateAndSave(MarketType.KOSPI, d1);
        verify(indicatorDailyCalculateService).calculateAndSave(MarketType.KOSPI, d2);
    }
}
