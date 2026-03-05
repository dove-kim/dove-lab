package com.dove.stockconsumer.listener;

import com.dove.eventsupport.domain.entity.FailedEvent;
import com.dove.eventsupport.domain.repository.FailedEventRepository;
import com.dove.stockdata.domain.repository.StockDataQueryRepository;
import com.dove.stockdata.application.dto.IndicatorCalcEventRequest;
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

/**
 * TECHNICAL_INDICATOR_CALC 토픽의 종목별 기술적 지표 계산 이벤트를 처리한다.
 * 지정된 날짜부터 최신까지 모든 거래일을 조회하여 순차적으로 지표를 계산한다.
 *
 * <p>Kafka 파티션 키가 stockCode이므로, 같은 종목의 이벤트는 동일 파티션에서
 * 순차 처리되어 계산 순서가 보장된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockIndicatorCalcEventListener {
    private static final String LISTENER_NAME = "StockIndicatorCalcEventListener";

    private final TechnicalIndicatorCalculationService calculationService;
    private final ObjectMapper objectMapper;
    private final StockDataQueryRepository stockDataQueryRepository;
    private final FailedEventRepository failedEventRepository;

    @KafkaListener(
            groupId = "stockIndicatorCalc-1",
            topics = "TECHNICAL_INDICATOR_CALC",
            concurrency = "4"
    )
    public void onIndicatorCalcEventRequest(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        try {
            String rawMessage = data.value();
            log.debug("종목별 지표 계산 요청 수신: {}", rawMessage);

            IndicatorCalcEventRequest request = objectMapper.readValue(
                    rawMessage, IndicatorCalcEventRequest.class);

            List<LocalDate> dates = stockDataQueryRepository.findTradeDatesFrom(
                    request.getMarketType(), request.getStockCode(), request.getTradeDate());

            log.debug("기술적 지표 계산 시작 - 시장: {}, 종목: {}, 시작일: {}, 계산 대상: {}일",
                    request.getMarketType(), request.getStockCode(), request.getTradeDate(), dates.size());

            for (LocalDate date : dates) {
                calculationService.calculateForStock(
                        request.getMarketType(), request.getStockCode(), date);
            }

            log.debug("기술적 지표 계산 완료 - 종목: {}, 계산일수: {}",
                    request.getStockCode(), dates.size());

        } catch (Exception e) {
            log.error("종목별 지표 계산 실패: topic={}, partition={}, offset={}, error={}",
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
}
