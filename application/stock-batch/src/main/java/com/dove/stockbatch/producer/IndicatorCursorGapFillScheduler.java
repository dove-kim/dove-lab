package com.dove.stockbatch.producer;

import com.dove.market.application.service.MarketDataCursorQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.dove.technicalindicator.application.service.IndicatorCursorQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** ACTIVE/SUSPENDED 전체 종목에 INDICATOR_CALC_TRIGGER 발행. 신규 지표 추가·누락 보정 모두 처리. 매 영업일 00:00 KST. */
@Slf4j
@Component
public class IndicatorCursorGapFillScheduler {

    private final IndicatorCursorQueryService indicatorCursorQueryService;
    private final MarketDataCursorQueryService marketDataCursorQueryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final List<MarketType> targetMarkets;

    public IndicatorCursorGapFillScheduler(
            IndicatorCursorQueryService indicatorCursorQueryService,
            MarketDataCursorQueryService marketDataCursorQueryService,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${krx.target-markets:KOSPI,KOSDAQ}") List<MarketType> targetMarkets) {
        this.indicatorCursorQueryService = indicatorCursorQueryService;
        this.marketDataCursorQueryService = marketDataCursorQueryService;
        this.kafkaTemplate = kafkaTemplate;
        this.targetMarkets = targetMarkets;
    }

    @Scheduled(cron = "${indicator.cursor.gap-fill-cron:0 0 0 * * *}", zone = "Asia/Seoul")
    public void fillGaps() {
        for (MarketType market : targetMarkets) {
            marketDataCursorQueryService.findLastProcessedDate(market)
                    .ifPresent(ignored -> fillGapsForMarket(market));
        }
    }

    private void fillGapsForMarket(MarketType market) {
        indicatorCursorQueryService.findEligibleStockCodes(market)
                .forEach(code -> publish(market, code));
    }

    private void publish(MarketType market, String code) {
        IndicatorCalcTrigger trigger = new IndicatorCalcTrigger(market, code);
        kafkaTemplate.send(IndicatorCalcTrigger.TOPIC, trigger.messageKey(), trigger);
    }
}
