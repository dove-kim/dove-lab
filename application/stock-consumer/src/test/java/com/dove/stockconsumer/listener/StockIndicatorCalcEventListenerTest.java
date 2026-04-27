package com.dove.stockconsumer.listener;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.technicalindicator.application.service.TechnicalIndicatorCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockIndicatorCalcEventListener 테스트")
class StockIndicatorCalcEventListenerTest {

    @Mock
    private TechnicalIndicatorCalculationService calculationService;
    @Mock
    private DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock
    private Acknowledgment acknowledgment;

    private StockIndicatorCalcEventListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        listener = new StockIndicatorCalcEventListener(calculationService, objectMapper, dailyStockPriceQueryService);
    }

    private ConsumerRecord<String, String> createRecord(String stockCode, String json) {
        return new ConsumerRecord<>("TECHNICAL_INDICATOR_CALC", 0, 0, stockCode, json);
    }

    @Test
    @DisplayName("지정 날짜부터 최신까지 거래일을 조회하여 순차 계산한다")
    void shouldFindTradeDatesFromAndCalculateSequentially() {
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240110\"}";
        when(dailyStockPriceQueryService.findTradeDatesFrom(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10)))
                .thenReturn(List.of(
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 11),
                        LocalDate.of(2024, 1, 12)));

        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        InOrder inOrder = inOrder(calculationService);
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 11));
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 12));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("거래일이 없으면 계산하지 않는다")
    void shouldNotCalculateWhenNoTradeDates() {
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240110\"}";
        when(dailyStockPriceQueryService.findTradeDatesFrom(any(), any(), any()))
                .thenReturn(List.of());

        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        verify(calculationService, never()).calculateForStock(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("계산 실패 → 로그 + ack")
    void shouldAckOnCalculationFailure() {
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240115\"}";
        when(dailyStockPriceQueryService.findTradeDatesFrom(any(), any(), any()))
                .thenReturn(List.of(LocalDate.of(2024, 1, 15)));
        doThrow(new RuntimeException("계산 오류"))
                .when(calculationService).calculateForStock(any(), any(), any());

        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("잘못된 메시지 → 계산 없이 ack")
    void shouldAckOnInvalidMessage() {
        listener.onIndicatorCalcEventRequest(createRecord("005930", "invalid json"), acknowledgment);

        verify(calculationService, never()).calculateForStock(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }
}
