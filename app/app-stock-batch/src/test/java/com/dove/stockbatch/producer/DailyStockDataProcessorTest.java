package com.dove.stockbatch.producer;

import com.dove.stockdata.application.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyStockDataProcessorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private DailyStockDataProcessor processor;

    @Test
    @DisplayName("KOSPI와 KOSDAQ 2건의 메시지를 전송한다")
    void shouldSendTwoMessagesForKospiAndKosdaq() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        verify(kafkaTemplate, times(2))
                .send(eq("KRX_STOCK_PRICE_QUERY"), any(String.class), any(KrxDailyStockDataRequest.class));
    }

    @Test
    @DisplayName("메시지 키에 시장 유형을 포함한다")
    void shouldIncludeMarketTypeInMessageKey() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2))
                .send(eq("KRX_STOCK_PRICE_QUERY"), keyCaptor.capture(), any(KrxDailyStockDataRequest.class));

        LocalDate expectedDate = LocalDate.now().minusDays(1);
        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys).containsExactlyInAnyOrder(
                String.format("daily-KOSPI-%s", expectedDate),
                String.format("daily-KOSDAQ-%s", expectedDate)
        );
    }

    @Test
    @DisplayName("eventVersion을 3으로 설정한다")
    void shouldSetEventVersion3InMessages() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<KrxDailyStockDataRequest> valueCaptor =
                ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);
        verify(kafkaTemplate, times(2))
                .send(any(), any(), valueCaptor.capture());

        valueCaptor.getAllValues().forEach(request ->
                assertThat(request.getEventVersion()).isEqualTo(3));
    }

    @Test
    @DisplayName("각 메시지에 올바른 시장 유형을 설정한다")
    void shouldSetCorrectMarketTypeInEachMessage() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<KrxDailyStockDataRequest> valueCaptor =
                ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);
        verify(kafkaTemplate, times(2))
                .send(any(), any(), valueCaptor.capture());

        List<KrxDailyStockDataRequest> requests = valueCaptor.getAllValues();
        assertThat(requests).extracting(KrxDailyStockDataRequest::getMarketType)
                .containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);

        LocalDate expectedDate = LocalDate.now().minusDays(1);
        requests.forEach(request ->
                assertThat(request.getBaseDate()).isEqualTo(expectedDate));
    }

    @Test
    @DisplayName("한 시장 전송이 실패해도 다른 시장은 전송한다")
    void shouldContinueSendingIfOneMarketFails() {
        // Given — 첫 번째 호출은 실패, 두 번째는 성공
        when(kafkaTemplate.send(eq("KRX_STOCK_PRICE_QUERY"), any(String.class), any(KrxDailyStockDataRequest.class)))
                .thenThrow(new RuntimeException("Kafka error"))
                .thenReturn(null);

        // When
        processor.sendDailyStockDataRequest();

        // Then — 2번 호출 시도
        verify(kafkaTemplate, times(2))
                .send(eq("KRX_STOCK_PRICE_QUERY"), any(String.class), any(KrxDailyStockDataRequest.class));
    }
}
