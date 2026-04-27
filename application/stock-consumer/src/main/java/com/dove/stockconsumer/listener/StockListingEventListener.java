package com.dove.stockconsumer.listener;

import com.dove.distributedlock.IdempotentConsumer;
import com.dove.stockconsumer.service.StockListingSyncService;
import com.dove.stock.application.dto.DailyStockListingQuery;
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
public class StockListingEventListener {
    private final StockListingSyncService stockListingSyncService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            groupId = "stockListing-1",
            topics = DailyStockListingQuery.TOPIC,
            concurrency = "4"
    )
    @IdempotentConsumer(prefix = "stock-listing", keyExpression = "#data.key()")
    public void onDailyStockListingQuery(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            DailyStockListingQuery query = parse(data.value());
            if (query == null) {
                return;
            }
            stockListingSyncService.syncForMarketAndDate(query.getMarketType(), query.getBaseDate());
        } catch (Exception e) {
            log.error("Unexpected error processing listing message: topic={}, partition={}, offset={}, error={}",
                    data.topic(), data.partition(), data.offset(), e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private DailyStockListingQuery parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, DailyStockListingQuery.class);
        } catch (Exception e) {
            log.error("Failed to parse DailyStockListingQuery: {}", e.getMessage());
            return null;
        }
    }
}
