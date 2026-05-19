package com.dove.scheduler.job;

import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.dto.ScanResult;
import com.dove.scheduler.service.IndicatorCompletionService;
import com.dove.scheduler.service.MarketDataCompletionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompletionScanJobTest {

    @Mock MarketDataCompletionService marketDataCompletionService;
    @Mock IndicatorCompletionService indicatorCompletionService;

    static final LocalDate TODAY        = LocalDate.of(2026, 4, 22);
    static final LocalDate UP_TO        = TODAY.minusDays(1);   // T+1
    static final LocalDate INITIAL_DATE = LocalDate.of(2010, 1, 1);

    static final int MAX_DATES_PER_RUN = 2500;

    CompletionScanJob buildJob(List<MarketType> markets) {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        return new CompletionScanJob(
                marketDataCompletionService, indicatorCompletionService,
                fixedClock, markets, INITIAL_DATE, MAX_DATES_PER_RUN, new ForkJoinPool(2));
    }

    @Test
    @DisplayName("upTo = today - 1 로 시장 데이터·지표 scan 호출 (T+1 정책)")
    void shouldUseYesterdayAsUpTo() {
        when(marketDataCompletionService.scan(any(), any(), any(), anyInt()))
                .thenAnswer(inv -> ScanResult.skipped(inv.getArgument(0)));

        buildJob(List.of(MarketType.KOSPI)).run();

        // maxDatesPerMarket = 2500 / 1 = 2500
        verify(marketDataCompletionService).scan(eq(MarketType.KOSPI), eq(INITIAL_DATE), eq(UP_TO), eq(2500));
        verify(indicatorCompletionService).scan(eq(MarketType.KOSPI), eq(INITIAL_DATE), eq(UP_TO));
    }

    @Test
    @DisplayName("모든 설정 시장에 대해 시장 데이터·지표 scan 호출")
    void shouldProcessAllConfiguredMarkets() {
        when(marketDataCompletionService.scan(any(), any(), any(), anyInt()))
                .thenAnswer(inv -> ScanResult.skipped(inv.getArgument(0)));

        buildJob(List.of(MarketType.KOSPI, MarketType.KOSDAQ, MarketType.KONEX)).run();

        verify(marketDataCompletionService, times(3)).scan(any(), any(), any(), anyInt());
        verify(indicatorCompletionService, times(3)).scan(any(), any(), any());
    }

}
