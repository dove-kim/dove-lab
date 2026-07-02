package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.kis.infrastructure.adapter.KisTradingDayAdapter;
import com.dove.market.application.service.ExchangeTradingDateService;
import com.dove.market.domain.enums.Exchange;
import com.dove.scheduler.fundamental.DailyValuationService;
import com.dove.scheduler.fundamental.FundamentalCollectionService;
import com.dove.scheduler.fundamental.ShareCountCollectionService;
import com.dove.scheduler.service.BreadthComputeService;
import com.dove.scheduler.service.IndicatorComputeService;
import com.dove.scheduler.service.ModelScoringService;
import com.dove.scheduler.service.RankComputeService;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.PriceCollectionService;
import com.dove.systemevent.application.service.SystemEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link DailyPipelineOrchestrator} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPipelineOrchestrator")
class DailyPipelineOrchestratorTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.now(CLOCK);

    @Mock
    private PriceCollectionService priceCollectionService;

    @Mock
    private IndicatorComputeService indicatorComputeService;

    @Mock
    private RankComputeService rankComputeService;

    @Mock
    private BreadthComputeService breadthComputeService;

    @Mock
    private ModelScoringService modelScoringService;

    @Mock
    private FundamentalCollectionService fundamentalCollectionService;

    @Mock
    private ShareCountCollectionService shareCountCollectionService;

    @Mock
    private DailyValuationService dailyValuationService;

    @Mock
    private SystemEventService systemEventService;

    @Mock
    private JobStatusRegistry jobStatusRegistry;

    @Mock
    private KisTradingDayAdapter tradingDayAdapter;

    @Mock
    private ExchangeTradingDateService tradingDateService;

    private DailyPipelineOrchestrator orchestrator() {
        return new DailyPipelineOrchestrator(priceCollectionService, indicatorComputeService,
                rankComputeService, breadthComputeService, modelScoringService,
                fundamentalCollectionService, shareCountCollectionService, dailyValuationService,
                systemEventService, jobStatusRegistry, tradingDayAdapter, tradingDateService, CLOCK);
    }

    @Nested
    @DisplayName("run() — 휴장일")
    class WhenHoliday {

        @Test
        @DisplayName("휴장일이면 수집·지표·rank 단계를 모두 스킵한다")
        void shouldSkipAllStagesWhenNotTradingDay() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(false);

            orchestrator().run();

            verify(priceCollectionService, never()).collect(any(), any(), any(), any(), any());
            verify(indicatorComputeService, never()).computeAll(any());
            verify(rankComputeService, never()).calculateAll(any());
            verify(breadthComputeService, never()).calculateAll(any());
            verify(tradingDateService, never()).register(any(), any());
        }
    }

    @Nested
    @DisplayName("run() — 거래일 정상")
    class WhenTradingDay {

        @Test
        @DisplayName("주가 → 지표 → rank 순서로 단계를 호출한다")
        void shouldRunStagesInOrderWhenTradingDay() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);

            orchestrator().run();

            for (StockExchange exchange : StockExchange.values()) {
                verify(priceCollectionService).collect(eq(exchange), eq(TODAY), eq(TODAY),
                        eq(CollectionProgress.NOOP), eq(DailyPriceFetcher.ADJUSTED_DATA_START));
            }
            InOrder inOrder = inOrder(priceCollectionService, indicatorComputeService,
                    rankComputeService, breadthComputeService, modelScoringService);
            inOrder.verify(priceCollectionService, times(StockExchange.values().length))
                    .collect(any(), any(), any(), any(), any());
            inOrder.verify(indicatorComputeService).computeAll(TODAY);
            inOrder.verify(rankComputeService).calculateAll(TODAY);
            inOrder.verify(breadthComputeService).calculateAll(TODAY);
            inOrder.verify(modelScoringService).scoreAll(TODAY);

            verify(tradingDateService).register(Exchange.KRX, TODAY);
            verify(tradingDateService).markPricesSynced(Exchange.KRX, TODAY);
            verify(systemEventService, never()).recordKisApiFailure(any(), any());
        }
    }

    @Nested
    @DisplayName("run() — 주가 수집 실패")
    class WhenPriceCollectionFails {

        @Test
        @DisplayName("일부 거래소 수집이 실패하면 이벤트를 기록하고 이후 단계를 스킵한다")
        void shouldRecordFailureAndSkipDownstreamWhenPriceFails() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);
            StockExchange first = StockExchange.values()[0];
            ParallelException failure = new ParallelException(new IllegalStateException("KIS 호출 실패"));
            doThrow(failure).when(priceCollectionService)
                    .collect(eq(first), any(), any(), any(), any());

            orchestrator().run();

            verify(systemEventService).recordKisApiFailure(first.name(), "KIS 호출 실패");
            verify(tradingDateService, never()).markPricesSynced(any(), any());
            verify(indicatorComputeService, never()).computeAll(any());
            verify(rankComputeService, never()).calculateAll(any());
            verify(breadthComputeService, never()).calculateAll(any());
        }
    }

    @Nested
    @DisplayName("run() — 지표 단계 실패")
    class WhenIndicatorStageFails {

        @Test
        @DisplayName("지표 계산이 실패해도 이벤트 기록 후 rank 단계를 계속 진행한다")
        void shouldRecordFailureAndContinueWhenIndicatorThrows() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);
            doThrow(new RuntimeException("지표 계산 오류")).when(indicatorComputeService).computeAll(TODAY);

            orchestrator().run();

            verify(systemEventService).recordPipelineStageFailure(
                    SchedulerJobName.INDICATOR.name(), "지표 계산 오류");
            verify(rankComputeService).calculateAll(TODAY);
        }
    }

    @Nested
    @DisplayName("run() — rank 단계 실패")
    class WhenRankStageFails {

        @Test
        @DisplayName("rank 계산이 실패해도 이벤트 기록 후 상승비율 단계를 계속 진행한다")
        void shouldRecordFailureAndContinueWhenRankThrows() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);
            doThrow(new RuntimeException("rank 계산 오류")).when(rankComputeService).calculateAll(TODAY);

            orchestrator().run();

            verify(systemEventService).recordPipelineStageFailure(
                    SchedulerJobName.RANK.name(), "rank 계산 오류");
            verify(breadthComputeService).calculateAll(TODAY);
        }
    }

    @Nested
    @DisplayName("run() — 상승비율 단계 실패")
    class WhenBreadthStageFails {

        @Test
        @DisplayName("상승비율 계산이 실패해도 이벤트 기록 후 모델 채점 단계를 계속 진행한다")
        void shouldRecordFailureAndContinueWhenBreadthThrows() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);
            doThrow(new RuntimeException("상승비율 계산 오류")).when(breadthComputeService).calculateAll(TODAY);

            orchestrator().run();

            verify(systemEventService).recordPipelineStageFailure(
                    SchedulerJobName.BREADTH.name(), "상승비율 계산 오류");
            verify(modelScoringService).scoreAll(TODAY);
        }
    }
}
