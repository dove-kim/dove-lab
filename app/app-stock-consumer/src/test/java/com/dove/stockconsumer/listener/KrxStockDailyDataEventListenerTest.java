package com.dove.stockconsumer.listener;

import com.dove.stockconsumer.dto.KrxDailyStockDataRequest;
import com.dove.krxmarketdata.service.KrxStockDailySaveService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;

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

    @InjectMocks
    private KrxStockDailyDataEventListener krxStockDailyDataEventListener;

    @Test
    @DisplayName("유효한 메시지를 성공적으로 처리해야 한다")
    void shouldProcessValidMessageSuccessfully() throws JsonProcessingException {
        // Given
        String validJson = "{\"eventVersion\":2,\"baseDate\":\"20230101\"}";
        ConsumerRecord<String, String> validRecord = new ConsumerRecord<>(
                "KRX_DATA_REQUEST", 0, 0, "key",
                validJson);

        KrxDailyStockDataRequest mockedRequest = new KrxDailyStockDataRequest(
                2, LocalDate.of(2023, 1, 1));
        when(objectMapper.readValue(eq(validJson), eq(KrxDailyStockDataRequest.class))).thenReturn(mockedRequest);

        // When
        krxStockDailyDataEventListener.krxStockDailyData(validRecord, acknowledgment);

        // Then
        verify(krxStockDailySaveService, times(1))
                .saveDailyMarketData(LocalDate.of(2023, 1, 1));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("JSON 파싱에 실패한 메시지를 건너뛰고 acknowledgment 해야 한다")
    void shouldSkipAndAcknowledgeOnParsingFailure() {
        // Given
        ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>(
                "KRX_DATA_REQUEST", 0, 1, "key",
                "invalid json string");

        // When
        krxStockDailyDataEventListener.krxStockDailyData(invalidRecord, acknowledgment);

        // Then
        verify(krxStockDailySaveService, never()).saveDailyMarketData(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("저장 중 예외가 발생해도 acknowledgment 해야 한다")
    void shouldAcknowledgeEvenWhenSaveThrowsException() throws JsonProcessingException {
        // Given
        String validJson = "{\"eventVersion\":2,\"baseDate\":\"20230101\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "KRX_DATA_REQUEST", 0, 0, "key", validJson);

        KrxDailyStockDataRequest mockedRequest = new KrxDailyStockDataRequest(
                2, LocalDate.of(2023, 1, 1));
        when(objectMapper.readValue(eq(validJson), eq(KrxDailyStockDataRequest.class))).thenReturn(mockedRequest);
        doThrow(new RuntimeException("DB connection failed"))
                .when(krxStockDailySaveService).saveDailyMarketData(any());

        // When
        krxStockDailyDataEventListener.krxStockDailyData(record, acknowledgment);

        // Then
        verify(acknowledgment, times(1)).acknowledge();
    }
}
