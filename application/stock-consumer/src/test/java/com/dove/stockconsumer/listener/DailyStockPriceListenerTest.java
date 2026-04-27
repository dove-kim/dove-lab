package com.dove.stockconsumer.listener;

import com.dove.stockconsumer.service.SaveDailyMarketDataService;
import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyStockPriceListenerTest {

    @Mock
    private SaveDailyMarketDataService saveDailyMarketDataService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Acknowledgment acknowledgment;

    private DailyStockPriceListener listener;

    @BeforeEach
    void setUp() {
        listener = new DailyStockPriceListener(saveDailyMarketDataService, objectMapper);
    }

    @Test
    @DisplayName("정상 메시지 → 유스케이스 호출 + ack")
    void shouldDelegateToUseCase() throws JsonProcessingException {
        String json = "{\"baseDate\":\"20260417\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                DailyStockPriceQuery.TOPIC, 0, 0, "key", json);
        DailyStockPriceQuery query = new DailyStockPriceQuery(LocalDate.of(2026, 4, 17), MarketType.KOSPI);
        when(objectMapper.readValue(eq(json), eq(DailyStockPriceQuery.class))).thenReturn(query);

        listener.onDailyStockPriceQuery(record, acknowledgment);

        verify(saveDailyMarketDataService).saveDailyMarketDataByMarket(LocalDate.of(2026, 4, 17), MarketType.KOSPI);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("파싱 실패 → 조용히 ack만")
    void shouldAckOnParseFailure() throws JsonProcessingException {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                DailyStockPriceQuery.TOPIC, 0, 1, "key", "invalid json");
        when(objectMapper.readValue(any(String.class), eq(DailyStockPriceQuery.class)))
                .thenThrow(new JsonProcessingException("bad") {});

        listener.onDailyStockPriceQuery(record, acknowledgment);

        verify(saveDailyMarketDataService, never()).saveDailyMarketDataByMarket(any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("유스케이스 예외 → 로깅 후 ack (재발행은 유스케이스의 pendingReFetch가 담당)")
    void shouldAckEvenOnUseCaseException() throws JsonProcessingException {
        String json = "{\"baseDate\":\"20260417\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                DailyStockPriceQuery.TOPIC, 0, 0, "key", json);
        when(objectMapper.readValue(eq(json), eq(DailyStockPriceQuery.class)))
                .thenReturn(new DailyStockPriceQuery(LocalDate.of(2026, 4, 17), MarketType.KOSPI));
        doThrow(new RuntimeException("db down"))
                .when(saveDailyMarketDataService).saveDailyMarketDataByMarket(any(), any());

        listener.onDailyStockPriceQuery(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
