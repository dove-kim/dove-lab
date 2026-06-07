package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.kis.infrastructure.adapter.KisTradingDayAdapter;
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

/**
 * 당일 주가 수집.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPriceJob {

    private final PriceCollectionService priceCollectionService;
    private final SystemEventService systemEventService;
    private final JobStatusRegistry jobStatusRegistry;
    private final KisTradingDayAdapter tradingDayAdapter;
    private final Clock clock;

    /**
     * 당일 거래소별 주가를 수집한다. 휴장일이면 건너뛴다.
     */
    @Scheduled(cron = "${daily.price.cron:0 0 21 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        log.info("DailyPriceJob 시작: {}", today);

        if (!tradingDayAdapter.isTradingDay(today)) {
            log.info("DailyPriceJob skip — 휴장일: {}", today);
            return;
        }

        jobStatusRegistry.start(SchedulerJobName.DAILY_PRICE.name(), StockExchange.values().length);

        int done = 0;
        for (StockExchange exchange : StockExchange.values()) {
            try {
                priceCollectionService.collect(exchange, today, today, CollectionProgress.NOOP,
                        DailyPriceFetcher.ADJUSTED_DATA_START);
            } catch (ParallelException e) {
                Throwable cause = e.getCause();
                log.error("[{}] 당일 수집 실패: {}", exchange, cause.getMessage(), cause);
                systemEventService.recordKisApiFailure(exchange.name(), cause.getMessage());
            }
            jobStatusRegistry.progress(SchedulerJobName.DAILY_PRICE.name(), ++done);
        }

        jobStatusRegistry.complete(SchedulerJobName.DAILY_PRICE.name());
        log.info("DailyPriceJob 완료: {}", today);
    }
}
