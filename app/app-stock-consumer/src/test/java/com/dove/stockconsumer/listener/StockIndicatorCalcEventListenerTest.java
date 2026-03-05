package com.dove.stockconsumer.listener;

import com.dove.eventsupport.domain.entity.FailedEvent;
import com.dove.eventsupport.domain.enums.FailedEventStatus;
import com.dove.eventsupport.domain.repository.FailedEventRepository;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataQueryRepository;
import com.dove.technicalindicator.application.service.TechnicalIndicatorCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockIndicatorCalcEventListener 테스트")
class StockIndicatorCalcEventListenerTest {

    @Mock
    private TechnicalIndicatorCalculationService calculationService;

    @Mock
    private StockDataQueryRepository stockDataQueryRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private FailedEventRepository failedEventRepository;

    private StockIndicatorCalcEventListener listener;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        listener = new StockIndicatorCalcEventListener(
                calculationService, objectMapper, stockDataQueryRepository, failedEventRepository);
    }

    private ConsumerRecord<String, String> createRecord(String stockCode, String json) {
        return new ConsumerRecord<>("TECHNICAL_INDICATOR_CALC", 0, 0, stockCode, json);
    }

    @Test
    @DisplayName("지정 날짜부터 최신까지 거래일을 조회하여 순차 계산한다")
    void shouldFindTradeDatesFromAndCalculateSequentially() throws Exception {
        // Given
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240110\"}";
        when(stockDataQueryRepository.findTradeDatesFrom(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10)))
                .thenReturn(List.of(
                        LocalDate.of(2024, 1, 10),
                        LocalDate.of(2024, 1, 11),
                        LocalDate.of(2024, 1, 12)));

        // When
        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        // Then
        InOrder inOrder = inOrder(calculationService);
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 11));
        inOrder.verify(calculationService).calculateForStock(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 12));
        verify(failedEventRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("거래일이 없으면 계산하지 않는다")
    void shouldNotCalculateWhenNoTradeDates() throws Exception {
        // Given
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240110\"}";
        when(stockDataQueryRepository.findTradeDatesFrom(any(), any(), any()))
                .thenReturn(List.of());

        // When
        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        // Then
        verify(calculationService, never()).calculateForStock(any(), any(), any());
        verify(failedEventRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("계산 실패 시 FailedEvent를 저장하고 acknowledge한다")
    void shouldSaveFailedEventOnCalculationFailure() throws Exception {
        // Given
        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240115\"}";
        when(stockDataQueryRepository.findTradeDatesFrom(any(), any(), any()))
                .thenReturn(List.of(LocalDate.of(2024, 1, 15)));
        doThrow(new RuntimeException("계산 오류"))
                .when(calculationService).calculateForStock(any(), any(), any());

        // When
        listener.onIndicatorCalcEventRequest(createRecord("005930", json), acknowledgment);

        // Then
        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        FailedEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("TECHNICAL_INDICATOR_CALC");
        assertThat(saved.getMessageKey()).isEqualTo("005930");
        assertThat(saved.getPayload()).isEqualTo(json);
        assertThat(saved.getErrorType()).isEqualTo("RuntimeException");
        assertThat(saved.getErrorMessage()).isEqualTo("계산 오류");
        assertThat(saved.getListenerName()).isEqualTo("StockIndicatorCalcEventListener");
        assertThat(saved.getStatus()).isEqualTo(FailedEventStatus.PENDING);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("잘못된 메시지 시 FailedEvent를 저장하고 acknowledge한다")
    void shouldSaveFailedEventOnInvalidMessage() throws Exception {
        // Given
        String invalidJson = "invalid json";

        // When
        listener.onIndicatorCalcEventRequest(createRecord("005930", invalidJson), acknowledgment);

        // Then
        verify(calculationService, never()).calculateForStock(any(), any(), any());

        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        FailedEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("TECHNICAL_INDICATOR_CALC");
        assertThat(saved.getPayload()).isEqualTo(invalidJson);
        assertThat(saved.getListenerName()).isEqualTo("StockIndicatorCalcEventListener");
        assertThat(saved.getStatus()).isEqualTo(FailedEventStatus.PENDING);
        verify(acknowledgment).acknowledge();
    }
}
