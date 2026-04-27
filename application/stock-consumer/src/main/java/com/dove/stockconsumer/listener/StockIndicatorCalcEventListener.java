package com.dove.stockconsumer.listener;

import com.dove.distributedlock.IdempotentConsumer;
import com.dove.technicalindicator.application.dto.IndicatorCalcEventRequest;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.technicalindicator.application.service.TechnicalIndicatorCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockIndicatorCalcEventListener {

    private final TechnicalIndicatorCalculationService calculationService;
    private final ObjectMapper objectMapper;
    private final DailyStockPriceQueryService dailyStockPriceQueryService;

    @KafkaListener(
            groupId = "stockIndicatorCalc-1",
            topics = "TECHNICAL_INDICATOR_CALC",
            concurrency = "4"
    )
    @IdempotentConsumer(prefix = "indicator-calc", keyExpression = "#data.key() + ':' + #data.value()")
    public void onIndicatorCalcEventRequest(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            IndicatorCalcEventRequest request = parse(data.value());
            if (request == null) {
                return;
            }

            List<LocalDate> dates = dailyStockPriceQueryService.findTradeDatesFrom(
                    request.getMarketType(), request.getStockCode(), request.getTradeDate());

            for (LocalDate date : dates) {
                calculationService.calculateForStock(
                        request.getMarketType(), request.getStockCode(), date);
            }
        } catch (Exception e) {
            log.error("종목별 지표 계산 실패: topic={}, partition={}, offset={}, error={}",
                    data.topic(), data.partition(), data.offset(), e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private IndicatorCalcEventRequest parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, IndicatorCalcEventRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse IndicatorCalcEventRequest: {}", e.getMessage());
            return null;
        }
    }
}
