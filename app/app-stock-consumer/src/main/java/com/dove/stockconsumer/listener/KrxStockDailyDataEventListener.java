package com.dove.stockconsumer.listener;

import com.dove.stockdata.application.dto.KrxDailyStockDataRequest;
import com.dove.krxmarketdata.application.service.KrxStockDailySaveService;
import com.dove.eventsupport.domain.entity.FailedEvent;
import com.dove.eventsupport.domain.repository.FailedEventRepository;
import com.dove.stockdata.domain.entity.StockDataChange;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataChangeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * KRX_STOCK_PRICE_QUERY 토픽의 한국거래소 주가조회 이벤트를 처리한다.
 * KRX API로 지정 시장의 주가 데이터를 조회하여 저장하고, 변경된 종목을 StockDataChange에 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxStockDailyDataEventListener {
    private static final String LISTENER_NAME = "KrxStockDailyDataEventListener";

    private final KrxStockDailySaveService krxStockDailySaveService;
    private final ObjectMapper objectMapper;
    private final StockDataChangeRepository stockDataChangeRepository;
    private final FailedEventRepository failedEventRepository;

    @KafkaListener(
            groupId = "krxDailyData-1",
            topics = "KRX_STOCK_PRICE_QUERY",
            concurrency = "4"
    )
    public void krxStockDailyData(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            String rawMessage = data.value();
            log.debug("Received raw message: {}", rawMessage);

            KrxDailyStockDataRequest request = parseMessage(rawMessage);
            if (request == null) {
                log.debug("Failed to parse message, skipping: topic={}, partition={}, offset={}",
                        data.topic(), data.partition(), data.offset());
                saveFailedEvent(data, "ParseFailure", "Failed to parse message");
                return;
            }

            List<String> stockCodes = krxStockDailySaveService.saveDailyMarketDataByMarket(
                    request.getBaseDate(), request.getMarketType());
            markStockDataChanges(stockCodes, request.getBaseDate(), request.getMarketType());

        } catch (Exception e) {
            log.error("Unexpected error processing message: topic={}, partition={}, offset={}, error={}",
                    data.topic(), data.partition(), data.offset(), e.getMessage(), e);
            saveFailedEvent(data, e.getClass().getSimpleName(), e.getMessage());
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private void saveFailedEvent(ConsumerRecord<String, String> data, String errorType, String errorMessage) {
        try {
            failedEventRepository.save(FailedEvent.of(
                    data.topic(), data.key(), data.value(),
                    errorType, errorMessage, LISTENER_NAME));
        } catch (Exception e) {
            log.error("Failed to save FailedEvent: {}", e.getMessage(), e);
        }
    }

    private void markStockDataChanges(List<String> stockCodes, LocalDate date, MarketType marketType) {
        for (String stockCode : stockCodes) {
            stockDataChangeRepository.save(new StockDataChange(marketType, stockCode, date));
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
