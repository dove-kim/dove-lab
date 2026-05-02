package com.dove.stockconsumer.listener;

import com.dove.distributedlock.IdempotentConsumer;
import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.stockconsumer.service.DailyMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMarketDataConsumer {

    private final DailyMarketDataService dailyMarketDataService;

    @KafkaListener(
            groupId = "marketData-1",
            topics = DailyMarketDataQuery.TOPIC,
            concurrency = "2",
            containerFactory = "dailyMarketDataFactory"
    )
    @IdempotentConsumer(prefix = "market-data", keyExpression = "#data.key()")
    public void onDailyMarketDataQuery(ConsumerRecord<String, DailyMarketDataQuery> data, Acknowledgment acknowledgment) {
        try {
            DailyMarketDataQuery query = data.value();
            dailyMarketDataService.process(query.getMarketType(), query.getBaseDate());
        } catch (Exception e) {
            log.error("시장 데이터 처리 실패: topic={}, key={}, error={}", data.topic(), data.key(), e.getMessage(), e);
            throw e;
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
