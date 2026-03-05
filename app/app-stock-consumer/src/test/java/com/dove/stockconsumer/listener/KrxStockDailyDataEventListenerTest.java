package com.dove.stockconsumer.listener;

import com.dove.stockdata.application.dto.KrxDailyStockDataRequest;
import com.dove.eventsupport.domain.entity.FailedEvent;
import com.dove.eventsupport.domain.enums.FailedEventStatus;
import com.dove.eventsupport.domain.repository.FailedEventRepository;
import com.dove.stockdata.domain.entity.StockDataChange;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataChangeRepository;
import com.dove.krxmarketdata.application.service.KrxStockDailySaveService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KrxStockDailyDataEventListener 테스트")
class KrxStockDailyDataEventListenerTest {

    @Mock
    private KrxStockDailySaveService krxStockDailySaveService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private StockDataChangeRepository stockDataChangeRepository;

    @Mock
    private FailedEventRepository failedEventRepository;

    @InjectMocks
    private KrxStockDailyDataEventListener krxStockDailyDataEventListener;

    @Nested
    @DisplayName("메시지 처리")
    class MessageProcessing {

        @Test
        @DisplayName("지정된 시장의 주가를 조회하고 변경을 기록한다")
        void shouldSaveAndRecordChangesForSpecifiedMarket() throws JsonProcessingException {
            // Given
            String json = "{\"eventVersion\":3,\"baseDate\":\"20231026\",\"marketType\":\"KOSPI\"}";
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "KRX_STOCK_PRICE_QUERY", 0, 0, "key", json);

            KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(
                    3, LocalDate.of(2023, 10, 26), MarketType.KOSPI);
            when(objectMapper.readValue(eq(json), eq(KrxDailyStockDataRequest.class)))
                    .thenReturn(request);
            when(krxStockDailySaveService.saveDailyMarketDataByMarket(
                    LocalDate.of(2023, 10, 26), MarketType.KOSPI))
                    .thenReturn(List.of("005930"));

            // When
            krxStockDailyDataEventListener.krxStockDailyData(record, acknowledgment);

            // Then
            verify(krxStockDailySaveService).saveDailyMarketDataByMarket(
                    LocalDate.of(2023, 10, 26), MarketType.KOSPI);

            ArgumentCaptor<StockDataChange> captor = ArgumentCaptor.forClass(StockDataChange.class);
            verify(stockDataChangeRepository).save(captor.capture());
            assertThat(captor.getValue().getId().getStockCode()).isEqualTo("005930");
            assertThat(captor.getValue().getId().getMarketType()).isEqualTo(MarketType.KOSPI);
            assertThat(captor.getValue().getId().getTradeDate()).isEqualTo(LocalDate.of(2023, 10, 26));
            verify(failedEventRepository, never()).save(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("휴장일이면 변경을 기록하지 않는다")
        void shouldNotRecordChangesOnHoliday() throws JsonProcessingException {
            // Given
            String json = "{\"eventVersion\":3,\"baseDate\":\"20231026\",\"marketType\":\"KOSPI\"}";
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "KRX_STOCK_PRICE_QUERY", 0, 0, "key", json);

            KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(
                    3, LocalDate.of(2023, 10, 26), MarketType.KOSPI);
            when(objectMapper.readValue(eq(json), eq(KrxDailyStockDataRequest.class)))
                    .thenReturn(request);
            when(krxStockDailySaveService.saveDailyMarketDataByMarket(any(), any()))
                    .thenReturn(List.of());

            // When
            krxStockDailyDataEventListener.krxStockDailyData(record, acknowledgment);

            // Then
            verify(stockDataChangeRepository, never()).save(any());
            verify(failedEventRepository, never()).save(any());
            verify(acknowledgment).acknowledge();
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandling {

        @Test
        @DisplayName("JSON 파싱에 실패하면 FailedEvent를 저장하고 acknowledge한다")
        void shouldSaveFailedEventOnParsingFailure() {
            // Given
            ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>(
                    "KRX_STOCK_PRICE_QUERY", 0, 1, "key",
                    "invalid json string");

            // When
            krxStockDailyDataEventListener.krxStockDailyData(invalidRecord, acknowledgment);

            // Then
            verify(krxStockDailySaveService, never()).saveDailyMarketDataByMarket(any(), any());
            verify(stockDataChangeRepository, never()).save(any(StockDataChange.class));

            ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
            verify(failedEventRepository).save(captor.capture());
            FailedEvent saved = captor.getValue();
            assertThat(saved.getTopic()).isEqualTo("KRX_STOCK_PRICE_QUERY");
            assertThat(saved.getMessageKey()).isEqualTo("key");
            assertThat(saved.getPayload()).isEqualTo("invalid json string");
            assertThat(saved.getErrorType()).isEqualTo("ParseFailure");
            assertThat(saved.getListenerName()).isEqualTo("KrxStockDailyDataEventListener");
            assertThat(saved.getStatus()).isEqualTo(FailedEventStatus.PENDING);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("처리 중 예외 발생 시 FailedEvent를 저장하고 acknowledge한다")
        void shouldSaveFailedEventOnProcessingException() throws JsonProcessingException {
            // Given
            String json = "{\"eventVersion\":3,\"baseDate\":\"20231026\",\"marketType\":\"KOSPI\"}";
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "KRX_STOCK_PRICE_QUERY", 0, 0, "key", json);

            KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(
                    3, LocalDate.of(2023, 10, 26), MarketType.KOSPI);
            when(objectMapper.readValue(eq(json), eq(KrxDailyStockDataRequest.class)))
                    .thenReturn(request);
            doThrow(new RuntimeException("DB error"))
                    .when(krxStockDailySaveService).saveDailyMarketDataByMarket(any(), any());

            // When
            krxStockDailyDataEventListener.krxStockDailyData(record, acknowledgment);

            // Then
            ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
            verify(failedEventRepository).save(captor.capture());
            FailedEvent saved = captor.getValue();
            assertThat(saved.getTopic()).isEqualTo("KRX_STOCK_PRICE_QUERY");
            assertThat(saved.getMessageKey()).isEqualTo("key");
            assertThat(saved.getPayload()).isEqualTo(json);
            assertThat(saved.getErrorType()).isEqualTo("RuntimeException");
            assertThat(saved.getErrorMessage()).isEqualTo("DB error");
            assertThat(saved.getListenerName()).isEqualTo("KrxStockDailyDataEventListener");
            assertThat(saved.getStatus()).isEqualTo(FailedEventStatus.PENDING);
            verify(acknowledgment).acknowledge();
        }
    }
}
