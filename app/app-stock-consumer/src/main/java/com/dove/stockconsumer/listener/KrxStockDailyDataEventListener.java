package com.dove.stockconsumer.listener;

import com.dove.stockconsumer.dto.KrxDailyStockDataRequest;
import com.dove.krxmarketdata.service.KrxStockDailySaveService;
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
public class KrxStockDailyDataEventListener {
    private final KrxStockDailySaveService krxStockDailySaveService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            groupId = "krxDailyData-1",
            topics = "KRX_DATA_REQUEST",
            concurrency = "3"
    )
    public void krxStockDailyData(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            String rawMessage = data.value();
            log.debug("Received raw message: {}", rawMessage);

            KrxDailyStockDataRequest request = parseMessage(rawMessage);
            if (request == null) {
                log.debug("Failed to parse message, skipping: topic={}, partition={}, offset={}",
                        data.topic(), data.partition(), data.offset());
                return;
            }

            krxStockDailySaveService.saveDailyMarketData(request.getBaseDate());

        } catch (Exception e) {
            log.error("Unexpected error processing message: topic={}, partition={}, offset={}, error={}",
                    data.topic(), data.partition(), data.offset(), e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private KrxDailyStockDataRequest parseMessage(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, KrxDailyStockDataRequest.class);
        } catch (Exception e) {
            log.error("Cannot parse message as KrxDailyStockDataRequest: {}, error: {}", rawMessage, e.getMessage());
            return null;
        }
    }

}
