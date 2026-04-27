package com.dove.stockconsumer.listener;

import com.dove.distributedlock.IdempotentConsumer;
import com.dove.stockconsumer.service.SaveDailyMarketDataService;
import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStockPriceListener {
    private final SaveDailyMarketDataService saveDailyMarketDataService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            groupId = "stockPrice-1",
            topics = DailyStockPriceQuery.TOPIC,
            concurrency = "4"
    )
    @IdempotentConsumer(prefix = "stock-price", keyExpression = "#data.key()")
    public void onDailyStockPriceQuery(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            DailyStockPriceQuery query = parse(data.value());
            if (query == null) {
                return;
            }
            saveDailyMarketDataService.saveDailyMarketDataByMarket(query.getBaseDate(), query.getMarketType());
        } catch (Exception e) {
            log.error("Unexpected error: topic={}, partition={}, offset={}, error={}",
                    data.topic(), data.partition(), data.offset(), e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private DailyStockPriceQuery parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, DailyStockPriceQuery.class);
        } catch (Exception e) {
            log.error("Failed to parse DailyStockPriceQuery: {}", e.getMessage());
            return null;
        }
    }
}
