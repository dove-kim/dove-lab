package com.dove.stockconsumer.listener;

import com.dove.technicalindicator.application.dto.IndicatorCalcRequest;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IndicatorCalcTriggerConsumer {

    private final List<TechnicalIndicatorCalculator> calculators;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            groupId = "indicatorCalc-1",
            topics = IndicatorCalcTrigger.TOPIC,
            concurrency = "4",
            containerFactory = "indicatorCalcTriggerFactory"
    )
    public void onTrigger(IndicatorCalcTrigger trigger, Acknowledgment acknowledgment) {
        try {
            calculators.forEach(calc -> {
                IndicatorCalcRequest req = new IndicatorCalcRequest(
                        trigger.getMarketType(), trigger.getStockCode(),
                        calc.cursorType(), trigger.getInsertedDate());
                kafkaTemplate.send(IndicatorCalcRequest.TOPIC, req.messageKey(), req);
            });
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
