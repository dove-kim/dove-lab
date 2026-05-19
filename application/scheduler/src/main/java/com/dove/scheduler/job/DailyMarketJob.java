package com.dove.scheduler.job;

import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.service.IndicatorDailyCalculateService;
import com.dove.scheduler.service.MarketDataCompletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/** 08:05 KST — 전 영업일 종목·주가를 수집한다. */
@Slf4j
@Component
public class DailyMarketJob {

    private final MarketDataCompletionService marketDataCompletionService;
    private final IndicatorDailyCalculateService indicatorDailyCalculateService;
    private final MarketTradingDateService marketTradingDateService;
    private final Clock clock;
    private final List<MarketType> targetMarkets;
    private final ForkJoinPool marketParallelPool;

    public DailyMarketJob(
            MarketDataCompletionService marketDataCompletionService,
            IndicatorDailyCalculateService indicatorDailyCalculateService,
            MarketTradingDateService marketTradingDateService,
            Clock clock,
            @Value("${krx.target-markets}") List<MarketType> targetMarkets,
            ForkJoinPool marketParallelPool) {
        this.marketDataCompletionService = marketDataCompletionService;
        this.indicatorDailyCalculateService = indicatorDailyCalculateService;
        this.marketTradingDateService = marketTradingDateService;
        this.clock = clock;
        this.targetMarkets = targetMarkets;
        this.marketParallelPool = marketParallelPool;
    }

    @Scheduled(cron = "${market.data.cron:0 5 8 * * MON-FRI}", zone = "Asia/Seoul")
    public void run() {
        // T+1: D일 데이터는 D+1일 08:00 이후 제공 → upTo = 전일
        LocalDate today = LocalDate.now(clock);
        LocalDate upTo = today.minusDays(1);
        log.info("DailyMarketJob 시작: upTo={}", upTo);

        marketParallelPool.submit(() ->
                targetMarkets.parallelStream()
                        .forEach(market -> processMarket(market, today, upTo))
        ).join();

        log.info("DailyMarketJob 완료");
    }

    private void processMarket(MarketType market, LocalDate today, LocalDate upTo) {
        // 커서 없음(신규 배포) → 오늘 기준 1년 전부터 수집
        LocalDate from = marketTradingDateService.findLastProcessedDate(market)
                .map(d -> d.plusDays(1))
                .orElse(today.minusYears(1));

        if (from.isAfter(upTo)) {
            log.info("[{}] 처리할 날짜 없음 (커서={}, upTo={})", market, from.minusDays(1), upTo);
            return;
        }

        log.info("[{}] 수집 시작: {}~{}", market, from, upTo);
        List<LocalDate> openDates = marketDataCompletionService.syncRange(market, from, upTo);
        openDates.forEach(date -> indicatorDailyCalculateService.calculateAndSave(market, date));
    }
}
