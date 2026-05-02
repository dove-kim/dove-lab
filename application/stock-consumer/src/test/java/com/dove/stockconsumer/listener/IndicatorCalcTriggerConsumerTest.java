package com.dove.stockconsumer.listener;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.application.dto.IndicatorCalcRequest;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorCalcTriggerConsumerTest {

    @Mock private TechnicalIndicatorCalculator calculator;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private Acknowledgment acknowledgment;

    private IndicatorCalcTriggerConsumer consumer;

    @BeforeEach
    void setUp() {
        when(calculator.cursorType()).thenReturn(IndicatorType.SMA_5);
        consumer = new IndicatorCalcTriggerConsumer(List.of(calculator), kafkaTemplate);
    }

    @Test
    @DisplayName("트리거 → calculator별 INDICATOR_CALC_REQUESTED 발행 + ack")
    void shouldFanOutPerCalculatorOnTrigger() {
        IndicatorCalcTrigger trigger = new IndicatorCalcTrigger(MarketType.KOSPI, "005930");

        consumer.onTrigger(trigger, acknowledgment);

        verify(kafkaTemplate).send(
                eq(IndicatorCalcRequest.TOPIC),
                eq("KOSPI-005930-SMA_5"),
                any(IndicatorCalcRequest.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("트리거 + insertedDate → per-calculator에 insertedDate 전달")
    void shouldPassInsertedDateInFanOut() {
        LocalDate date = LocalDate.of(2026, 4, 21);
        IndicatorCalcTrigger trigger = new IndicatorCalcTrigger(MarketType.KOSPI, "005930", date);

        consumer.onTrigger(trigger, acknowledgment);

        ArgumentCaptor<IndicatorCalcRequest> captor = ArgumentCaptor.forClass(IndicatorCalcRequest.class);
        verify(kafkaTemplate).send(eq(IndicatorCalcRequest.TOPIC), any(), captor.capture());
        assertThat(captor.getValue().getInsertedDate()).isEqualTo(date);
        verify(acknowledgment).acknowledge();
    }
}
