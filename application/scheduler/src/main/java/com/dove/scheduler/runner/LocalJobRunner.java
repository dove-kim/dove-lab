package com.dove.scheduler.runner;

import com.dove.scheduler.job.DailyPriceJob;
import com.dove.scheduler.job.IndicatorJob;
import com.dove.scheduler.job.StockDetailJob;
import com.dove.scheduler.job.StockSyncJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 Job 실행기 (local 프로파일에서만 활성화).
 * JOB=stock-sync | stock-detail | daily-price | indicator
 */
@Slf4j
@Component
@Profile("local")
public class LocalJobRunner implements ApplicationRunner {

    private final StockSyncJob stockSyncJob;
    private final StockDetailJob stockDetailJob;
    private final DailyPriceJob dailyPriceJob;
    private final IndicatorJob indicatorJob;
    private final ApplicationContext context;
    private final String job;

    public LocalJobRunner(StockSyncJob stockSyncJob, StockDetailJob stockDetailJob,
                          DailyPriceJob dailyPriceJob, IndicatorJob indicatorJob,
                          ApplicationContext context, @Value("${JOB:}") String job) {
        this.stockSyncJob = stockSyncJob;
        this.stockDetailJob = stockDetailJob;
        this.dailyPriceJob = dailyPriceJob;
        this.indicatorJob = indicatorJob;
        this.context = context;
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (job.isBlank()) {
            log.warn("[로컬] JOB 환경변수 없음. stock-sync | stock-detail | daily-price | indicator 중 선택");
            exit(1); return;
        }
        switch (job) {
            case "stock-sync"   -> stockSyncJob.run();
            case "stock-detail" -> stockDetailJob.run();
            case "daily-price"  -> dailyPriceJob.run();
            case "indicator"    -> indicatorJob.run();
            default -> { log.error("[로컬] 알 수 없는 JOB: {}", job); exit(1); return; }
        }
        exit(0);
    }

    private void exit(int code) {
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
