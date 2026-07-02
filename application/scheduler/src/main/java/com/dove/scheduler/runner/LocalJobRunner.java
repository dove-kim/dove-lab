package com.dove.scheduler.runner;

import com.dove.fundamental.domain.enums.ReportCode;
import com.dove.scheduler.fundamental.DailyValuationService;
import com.dove.scheduler.fundamental.FundamentalCollectionService;
import com.dove.scheduler.fundamental.ShareCountCollectionService;
import com.dove.scheduler.job.DailyPipelineOrchestrator;
import com.dove.scheduler.job.StockDetailJob;
import com.dove.scheduler.job.StockSyncJob;
import com.dove.scheduler.service.BreadthComputeService;
import com.dove.scheduler.service.IndicatorComputeService;
import com.dove.scheduler.service.ModelScoringService;
import com.dove.scheduler.service.RankComputeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 로컬 개발용 Job 실행기 (local 프로파일에서만 활성화).
 * JOB=pipeline | derived | indicator | rank | breadth | model-score | stock-sync | stock-detail
 * (pipeline = 전체 일일 파이프라인, derived = 지표→rank→breadth 무-KIS 연쇄). 날짜는 JOB_DATE(yyyy-MM-dd)로 고정.
 */
@Slf4j
@Component
@Profile("local")
public class LocalJobRunner implements ApplicationRunner {

    private final DailyPipelineOrchestrator pipelineOrchestrator;
    private final IndicatorComputeService indicatorComputeService;
    private final RankComputeService rankComputeService;
    private final BreadthComputeService breadthComputeService;
    private final ModelScoringService modelScoringService;
    private final StockSyncJob stockSyncJob;
    private final StockDetailJob stockDetailJob;
    private final FundamentalCollectionService fundamentalCollectionService;
    private final ShareCountCollectionService shareCountCollectionService;
    private final DailyValuationService dailyValuationService;
    private final Clock clock;
    private final ApplicationContext context;
    private final String job;
    private final int fundFromYear;
    private final int fundToYear;

    public LocalJobRunner(DailyPipelineOrchestrator pipelineOrchestrator,
                          IndicatorComputeService indicatorComputeService, RankComputeService rankComputeService,
                          BreadthComputeService breadthComputeService, ModelScoringService modelScoringService,
                          StockSyncJob stockSyncJob, StockDetailJob stockDetailJob,
                          FundamentalCollectionService fundamentalCollectionService,
                          ShareCountCollectionService shareCountCollectionService,
                          DailyValuationService dailyValuationService, Clock clock,
                          ApplicationContext context, @Value("${JOB:}") String job,
                          @Value("${FUND_FROM_YEAR:2015}") int fundFromYear,
                          @Value("${FUND_TO_YEAR:2024}") int fundToYear) {
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.indicatorComputeService = indicatorComputeService;
        this.rankComputeService = rankComputeService;
        this.breadthComputeService = breadthComputeService;
        this.modelScoringService = modelScoringService;
        this.stockSyncJob = stockSyncJob;
        this.stockDetailJob = stockDetailJob;
        this.fundamentalCollectionService = fundamentalCollectionService;
        this.shareCountCollectionService = shareCountCollectionService;
        this.dailyValuationService = dailyValuationService;
        this.clock = clock;
        this.context = context;
        this.job = job;
        this.fundFromYear = fundFromYear;
        this.fundToYear = fundToYear;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (job.isBlank()) {
            log.warn("[로컬] JOB 환경변수 없음. pipeline | derived | indicator | rank | breadth | model-score | stock-sync | stock-detail | fund-corp-sync | fund-backfill | fund-poll | share-count | share-count-range | valuation | valuation-range 중 선택");
            exit(1); return;
        }
        LocalDate today = LocalDate.now(clock);
        switch (job) {
            case "pipeline"     -> pipelineOrchestrator.run();
            case "indicator"    -> indicatorComputeService.computeAll(today);
            case "rank"         -> rankComputeService.calculateAll(today);
            case "breadth"      -> breadthComputeService.calculateAll(today);
            case "model-score"  -> modelScoringService.scoreAll(today);
            case "derived"      -> {
                indicatorComputeService.computeAll(today);
                rankComputeService.calculateAll(today);
                breadthComputeService.calculateAll(today);
            }
            case "stock-sync"   -> stockSyncJob.run();
            case "stock-detail" -> stockDetailJob.run();
            case "fund-corp-sync" -> fundamentalCollectionService.syncCorpCodes();
            case "fund-backfill"  -> fundamentalCollectionService.backfill(fundFromYear, fundToYear, List.of(ReportCode.ANNUAL), com.dove.stockcollection.application.service.CollectionProgress.NOOP);
            case "fund-poll"      -> fundamentalCollectionService.pollRecent(today.minusDays(7), today);
            case "valuation"      -> dailyValuationService.compute(today);
            case "valuation-range" -> dailyValuationService.computeRange(
                    LocalDate.of(fundFromYear, 1, 1), LocalDate.of(fundToYear, 12, 31),
                    com.dove.stockcollection.application.service.CollectionProgress.NOOP);
            case "share-count"    -> shareCountCollectionService.collect(
                    today.minusDays(14), today,
                    com.dove.stockcollection.application.service.CollectionProgress.NOOP);
            case "share-count-range" -> shareCountCollectionService.collect(
                    LocalDate.of(fundFromYear, 1, 1), LocalDate.of(fundToYear, 12, 31),
                    com.dove.stockcollection.application.service.CollectionProgress.NOOP);
            default -> { log.error("[로컬] 알 수 없는 JOB: {}", job); exit(1); return; }
        }
        exit(0);
    }

    private void exit(int code) {
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
