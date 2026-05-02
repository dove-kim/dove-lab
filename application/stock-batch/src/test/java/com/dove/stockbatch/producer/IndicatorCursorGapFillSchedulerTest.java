package com.dove.stockbatch.producer;

import com.dove.market.application.service.MarketDataCursorQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.dove.technicalindicator.application.service.IndicatorCursorQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorCursorGapFillSchedulerTest {

    @Mock private IndicatorCursorQueryService indicatorCursorQueryService;
    @Mock private MarketDataCursorQueryService marketDataCursorQueryService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private final LocalDate lastProcessedDate = LocalDate.of(2026, 4, 17);
    private IndicatorCursorGapFillScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new IndicatorCursorGapFillScheduler(
                indicatorCursorQueryService, marketDataCursorQueryService,
                kafkaTemplate, List.of(MarketType.KOSPI));
    }

    @Test
    @DisplayName("eligible 종목 있음 → INDICATOR_CALC_TRIGGER 발행")
    void shouldPublishForEligibleCodes() {
        when(marketDataCursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(lastProcessedDate));
        when(indicatorCursorQueryService.findEligibleStockCodes(MarketType.KOSPI))
                .thenReturn(List.of("005930", "000660"));

        scheduler.fillGaps();

        verify(kafkaTemplate, times(2)).send(eq(IndicatorCalcTrigger.TOPIC), any(), any(IndicatorCalcTrigger.class));
    }

    @Test
    @DisplayName("eligible 종목 없음 → 발행 없음")
    void shouldNotPublishWhenNoEligibleCodes() {
        when(marketDataCursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(lastProcessedDate));
        when(indicatorCursorQueryService.findEligibleStockCodes(MarketType.KOSPI))
                .thenReturn(List.of());

        scheduler.fillGaps();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("MarketDataCursor 없음 → 조회·발행 없음")
    void shouldSkipWhenNoCursor() {
        when(marketDataCursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.empty());

        scheduler.fillGaps();

        verify(indicatorCursorQueryService, never()).findEligibleStockCodes(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("발행 시 key={market}-{stockCode}, topic=INDICATOR_CALC_TRIGGER")
    void shouldPublishWithCorrectKeyAndTopic() {
        when(marketDataCursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(lastProcessedDate));
        when(indicatorCursorQueryService.findEligibleStockCodes(MarketType.KOSPI))
                .thenReturn(List.of("005930"));

        scheduler.fillGaps();

        verify(kafkaTemplate).send(eq(IndicatorCalcTrigger.TOPIC), eq("KOSPI-005930"), any(IndicatorCalcTrigger.class));
    }

    @Test
    @DisplayName("여러 시장 — 각 시장 독립적으로 트리거")
    void shouldFillGapsForEachMarketIndependently() {
        scheduler = new IndicatorCursorGapFillScheduler(
                indicatorCursorQueryService, marketDataCursorQueryService,
                kafkaTemplate, List.of(MarketType.KOSPI, MarketType.KOSDAQ));
        when(marketDataCursorQueryService.findLastProcessedDate(any()))
                .thenReturn(Optional.of(lastProcessedDate));
        when(indicatorCursorQueryService.findEligibleStockCodes(MarketType.KOSPI))
                .thenReturn(List.of("005930"));
        when(indicatorCursorQueryService.findEligibleStockCodes(MarketType.KOSDAQ))
                .thenReturn(List.of("293490"));

        scheduler.fillGaps();

        verify(kafkaTemplate, times(2)).send(eq(IndicatorCalcTrigger.TOPIC), any(), any(IndicatorCalcTrigger.class));
    }
}
