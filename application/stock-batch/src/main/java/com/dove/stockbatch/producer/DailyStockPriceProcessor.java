package com.dove.stockbatch.producer;

import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.market.domain.enums.MarketType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** 08:10 KST에 today-1 날짜로 STOCK_PRICE_QUERY 이벤트 발행. 시장당 1건. */
@Slf4j
@Component
public class DailyStockPriceProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;
    private final List<MarketType> targetMarkets;

    public DailyStockPriceProcessor(
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${krx.target-markets:KOSPI,KOSDAQ}") List<MarketType> targetMarkets) {
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.targetMarkets = targetMarkets;
    }

    @Scheduled(cron = "0 10 8 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate targetDate = LocalDate.now(clock).minusDays(1);
        log.info("주가 조회 메시지 발행: targetDate={}", targetDate);
        for (MarketType marketType : targetMarkets) {
            try {
                String key = String.format("daily-%s-%s", marketType, targetDate);
                kafkaTemplate.send(DailyStockPriceQuery.TOPIC, key,
                        new DailyStockPriceQuery(targetDate, marketType));
            } catch (Exception e) {
                log.error("Kafka 메시지 전송 실패 - 시장: {}, 날짜: {}, 오류: {}",
                        marketType, targetDate, e.getMessage(), e);
            }
        }
    }
}
