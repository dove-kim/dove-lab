package com.dove.stockconsumer.listener;

import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockconsumer.service.DailyMarketDataService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyMarketDataConsumerTest {

    @Mock private DailyMarketDataService dailyMarketDataService;
    @Mock private Acknowledgment acknowledgment;

    private DailyMarketDataConsumer consumer;

    private final LocalDate date = LocalDate.of(2026, 4, 17);

    @BeforeEach
    void setUp() {
        consumer = new DailyMarketDataConsumer(dailyMarketDataService);
    }

    private ConsumerRecord<String, DailyMarketDataQuery> record(MarketType market, LocalDate d) {
        DailyMarketDataQuery query = new DailyMarketDataQuery(market, d);
        return new ConsumerRecord<>(DailyMarketDataQuery.TOPIC, 0, 0, query.partitionKey(), query);
    }

    @Test
    @DisplayName("정상 메시지 → process 호출 + acknowledge")
    void shouldCallProcessAndAcknowledge() {
        consumer.onDailyMarketDataQuery(record(MarketType.KOSPI, date), acknowledgment);

        verify(dailyMarketDataService).process(MarketType.KOSPI, date);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("process 예외(RetryLater) → acknowledge 후 예외 재throw")
    void shouldAcknowledgeAndRethrowOnServiceException() {
        doThrow(new IllegalStateException("RetryLater"))
                .when(dailyMarketDataService).process(any(), any());

        assertThatThrownBy(() ->
                consumer.onDailyMarketDataQuery(record(MarketType.KOSPI, date), acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("KOSDAQ 메시지 → process에 KOSDAQ 전달")
    void shouldDelegateCorrectMarketType() {
        consumer.onDailyMarketDataQuery(record(MarketType.KOSDAQ, date), acknowledgment);

        verify(dailyMarketDataService).process(MarketType.KOSDAQ, date);
        verify(acknowledgment).acknowledge();
    }
}
