package com.dove.scheduler.job;

import com.dove.concurrent.Parallel;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 일일 파이프라인을 단일 진입점에서 순차 호출하는 오케스트레이터.
 *
 * <p>주가 수집 → 지표 계산 → rank 계산 → 상승비율 계산 → 모델 채점을 순서대로 호출한다. 각 단계는 독립 커밋·커서를
 * 유지하므로 이 메서드에는 트랜잭션을 걸지 않는다. 단계 실패는 시스템 이벤트로 기록하며,
 * 주가 단계가 실패하면 이후 단계를 건너뛰고, 이후 단계 실패는 다음 단계를 막지 않는다(커서 멱등).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPipelineOrchestrator {

    private final PriceCollectionService priceCollectionService;
    private final IndicatorComputeService indicatorComputeService;
    private final RankComputeService rankComputeService;
    private final BreadthComputeService breadthComputeService;
    private final ModelScoringService modelScoringService;
    private final FundamentalCollectionService fundamentalCollectionService;
    private final ShareCountCollectionService shareCountCollectionService;
    private final DailyValuationService dailyValuationService;
    private final SystemEventService systemEventService;
    private final JobStatusRegistry jobStatusRegistry;
    private final KisTradingDayAdapter tradingDayAdapter;
    private final ExchangeTradingDateService tradingDateService;
    private final Clock clock;

    /**
     * 일일 파이프라인을 순차 실행한다. 휴장일이면 전체를 건너뛴다.
     */
    @Scheduled(cron = "${daily.price.cron:0 0 21 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        log.info("DailyPipelineOrchestrator 시작: {}", today);

        if (!tradingDayAdapter.isTradingDay(today)) {
            log.info("DailyPipelineOrchestrator skip — 휴장일: {}", today);
            return;
        }

        // ⓪ 주가 수집 — 실패 시 이후 단계 스킵
        if (!collectPrices(today)) {
            log.warn("DailyPipelineOrchestrator 중단 — 주가 수집 실패: {}", today);
            return;
        }

        // 두 묶음을 병렬 실행 — 지표계열(①②③)과 재무계열(⑤⑥)은 서로 다른 테이블을 쓰고 결과를 공유하지 않는다.
        //  A: 지표 → rank → 상승비율   B: 재무 폴링 → 밸류에이션(밸류에이션 전에 오늘 공시 반영)
        // 각 단계는 runStage로 실패 격리되어 브랜치·상대 브랜치를 막지 않는다.
        Runnable derivedBranch = () -> {
            runStage(SchedulerJobName.INDICATOR, () -> indicatorComputeService.computeAll(today));
            runStage(SchedulerJobName.RANK, () -> rankComputeService.calculateAll(today));
            runStage(SchedulerJobName.BREADTH, () -> breadthComputeService.calculateAll(today));
        };
        Runnable fundamentalBranch = () -> {
            // 14일 창 — 주말·연휴 + 서버 다운/조회 실패로 며칠 걸러도 다음 정상 실행에서 회수(멱등이라 겹침 안전)
            runStage(SchedulerJobName.FUNDAMENTAL_POLL, () -> fundamentalCollectionService.pollRecent(today.minusDays(14), today));
            // 상장주식수도 14일 창으로 회수(서버 다운·배포 누락 대비). 밸류에이션 시총 입력.
            runStage(SchedulerJobName.SHARE_COUNT, () -> shareCountCollectionService.collect(today.minusDays(14), today, CollectionProgress.NOOP));
            // 밸류에이션도 14일 창 재계산 — 실행 누락일 자동 회수 + 이번에 새로 들어온 공시를 그 공시일~오늘 구간에 반영(멱등 upsert).
            runStage(SchedulerJobName.VALUATION, () -> dailyValuationService.computeRange(today.minusDays(14), today, CollectionProgress.NOOP));
        };
        Parallel.run(List.of(derivedBranch, fundamentalBranch), 2, Runnable::run);

        // ④ 모델 채점 — 지표(①) 결과 필요 → 두 묶음 병렬 종료 후 실행. 실패해도 무방(커서 멱등).
        runStage(SchedulerJobName.MODEL_SCORING, () -> modelScoringService.scoreAll(today));

        log.info("DailyPipelineOrchestrator 완료: {}", today);
    }

    /**
     * 당일 거래소별 주가를 수집한다. 전 거래소 성공 시 true.
     */
    private boolean collectPrices(LocalDate today) {
        tradingDateService.register(Exchange.KRX, today);
        jobStatusRegistry.start(SchedulerJobName.DAILY_PRICE.name(), StockExchange.values().length);

        boolean allSynced = true;
        int done = 0;
        for (StockExchange exchange : StockExchange.values()) {
            try {
                priceCollectionService.collect(exchange, today, today, CollectionProgress.NOOP,
                        DailyPriceFetcher.ADJUSTED_DATA_START);
            } catch (ParallelException e) {
                Throwable cause = e.getCause();
                log.error("[{}] 당일 수집 실패: {}", exchange, cause.getMessage(), cause);
                systemEventService.recordKisApiFailure(exchange.name(), cause.getMessage());
                allSynced = false;
            }
            jobStatusRegistry.progress(SchedulerJobName.DAILY_PRICE.name(), ++done);
        }

        if (allSynced) {
            tradingDateService.markPricesSynced(Exchange.KRX, today);
        }
        jobStatusRegistry.complete(SchedulerJobName.DAILY_PRICE.name());
        return allSynced;
    }

    /**
     * 단계를 실행하고, 예외 발생 시 시스템 이벤트로 기록한다(다음 단계는 계속).
     */
    private void runStage(SchedulerJobName stage, Runnable step) {
        try {
            step.run();
        } catch (Exception e) {
            log.error("[{}] 파이프라인 단계 실패: {}", stage, e.getMessage(), e);
            systemEventService.recordPipelineStageFailure(stage.name(), e.getMessage());
        }
    }
}
