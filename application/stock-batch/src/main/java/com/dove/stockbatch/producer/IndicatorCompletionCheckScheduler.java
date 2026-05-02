package com.dove.stockbatch.producer;

import com.dove.market.application.service.MarketDataCursorQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.application.service.IndicatorCursorQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 08:00~08:59 매 1분 — 지표 계산 완료 여부 확인 후 완료 시점 로그. */
@Slf4j
@Component
public class IndicatorCompletionCheckScheduler {

    private final MarketDataCursorQueryService marketDataCursorQueryService;
    private final IndicatorCursorQueryService indicatorCursorQueryService;
    private final List<MarketType> targetMarkets;

    public IndicatorCompletionCheckScheduler(
            MarketDataCursorQueryService marketDataCursorQueryService,
            IndicatorCursorQueryService indicatorCursorQueryService,
            @Value("${krx.target-markets:KOSPI,KOSDAQ}") List<MarketType> targetMarkets) {
        this.marketDataCursorQueryService = marketDataCursorQueryService;
        this.indicatorCursorQueryService = indicatorCursorQueryService;
        this.targetMarkets = targetMarkets;
    }

    @Scheduled(cron = "0 * 8 * * *", zone = "Asia/Seoul")
    public void check() {
        for (MarketType market : targetMarkets) {
            marketDataCursorQueryService.findLastProcessedDate(market).ifPresent(marketCursor -> {
                long lagging = indicatorCursorQueryService.countLagging(market, marketCursor);
                if (lagging == 0) {
                    log.info("[지표 계산 완료] market={}, date={}", market, marketCursor);
                }
            });
        }
    }
}
