package com.dove.scheduler.job;

import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.dto.ScanResult;
import com.dove.scheduler.service.IndicatorCompletionService;
import com.dove.scheduler.service.MarketDataCompletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * 12:00 KST — 누락된 시장 데이터를 탐지·보정한다.
 *
 * <p>초기 날짜부터 전일까지 전체 구간을 스캔하되,
 * KRX API 일일 호출 한도를 준수하기 위해 시장당 최대 처리 날짜 수를 제한한다.
 * 잔여 누락분은 다음 실행(익일 12:00)으로 자동 이월된다.
 */
@Slf4j
@Component
public class CompletionScanJob {

    private final MarketDataCompletionService marketDataCompletionService;
    private final IndicatorCompletionService indicatorCompletionService;
    private final Clock clock;
    private final List<MarketType> targetMarkets;
    private final LocalDate initialDate;
    private final int maxDatesPerMarket;
    private final ForkJoinPool marketParallelPool;

    public CompletionScanJob(
            MarketDataCompletionService marketDataCompletionService,
            IndicatorCompletionService indicatorCompletionService,
            Clock clock,
            @Value("${krx.target-markets}") List<MarketType> targetMarkets,
            @Value("${market.data.initial-date:2010-01-01}") LocalDate initialDate,
            @Value("${completion.scan.max-dates-per-run:2500}") int maxDatesPerRun,
            ForkJoinPool marketParallelPool) {
        this.marketDataCompletionService = marketDataCompletionService;
        this.indicatorCompletionService = indicatorCompletionService;
        this.clock = clock;
        this.targetMarkets = targetMarkets;
        this.initialDate = initialDate;
        this.maxDatesPerMarket = maxDatesPerRun / targetMarkets.size();
        this.marketParallelPool = marketParallelPool;
    }

    @Scheduled(cron = "${completion.scan.cron:0 0 12 * * *}", zone = "Asia/Seoul")
    public void run() {
        // T+1: 전일까지 데이터 보정
        LocalDate upTo = LocalDate.now(clock).minusDays(1);
        log.info("CompletionScanJob 시작: {}~{} (시장당 최대 {}일)", initialDate, upTo, maxDatesPerMarket);

        marketParallelPool.submit(() ->
                targetMarkets.parallelStream()
                        .forEach(market -> processMarket(market, upTo))
        ).join();

        log.info("CompletionScanJob 전체 완료");
    }

    private void processMarket(MarketType market, LocalDate upTo) {
        ScanResult marketScan = marketDataCompletionService.scan(market, initialDate, upTo, maxDatesPerMarket);
        report(market, marketScan);
        indicatorCompletionService.scan(market, initialDate, upTo);
    }

    private void report(MarketType market, ScanResult r) {
        if (r.hasFailures()) {
            log.warn("[{}] 시장 데이터 보정: {}/{}건, 실패: {}", market, r.succeeded(), r.total(), r.failures());
        } else {
            log.info("[{}] 시장 데이터 보정: {}/{}건", market, r.succeeded(), r.total());
        }
    }
}
