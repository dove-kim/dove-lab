package com.dove.scheduler.job;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.market.domain.enums.MarketType;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.scheduler.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 당일 KRX 종목 수집. 최근 거래일 스냅샷을 최신 상태로 반영한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncJob {

    private final StockSyncService stockSyncService;
    private final JobStatusRegistry jobStatusRegistry;
    private final Clock clock;

    /**
     * 최근 N일치 KRX 종목을 시장별로 동기화한다.
     */
    @Scheduled(cron = "${stock.sync.cron:0 5 8 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(30);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        long total = days * MarketType.KRX_MARKETS.size();
        log.info("StockSyncJob 시작: {}~{} ({}일 × {}시장 = {}건)", from, to, days, MarketType.KRX_MARKETS.size(), total);
        jobStatusRegistry.start(SchedulerJobName.STOCK_SYNC.name(), total);
        try {
            AtomicLong done = new AtomicLong(0);
            for (MarketType market : MarketType.KRX_MARKETS) {
                stockSyncService.syncRange(market, from, to,
                        () -> jobStatusRegistry.progress(SchedulerJobName.STOCK_SYNC.name(), done.incrementAndGet()));
            }
            jobStatusRegistry.complete(SchedulerJobName.STOCK_SYNC.name());
        } catch (RuntimeException e) {
            jobStatusRegistry.fail(SchedulerJobName.STOCK_SYNC.name(), e.getMessage());
            throw e;
        }
        log.info("StockSyncJob 완료");
    }
}
