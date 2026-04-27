package com.dove.stockbatch.producer;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.eventretry.application.service.PendingEventRetryCommandService;
import com.dove.eventretry.application.service.PendingEventRetryQueryService;
import com.dove.eventretry.domain.entity.PendingEventRetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingEventRetryProcessorTest {

    @Mock
    private PendingEventRetryQueryService pendingEventRetryQueryService;
    @Mock
    private PendingEventRetryCommandService pendingEventRetryCommandService;
    @Mock
    private FailedEventCommandService failedEventCommandService;
    @Mock
    private KafkaTemplate<String, String> stringKafkaTemplate;

    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private PendingEventRetryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PendingEventRetryProcessor(
                pendingEventRetryQueryService, pendingEventRetryCommandService,
                failedEventCommandService, stringKafkaTemplate, clock);
    }

    private PendingEventRetry pending(int retryCount, Instant firstRequested) {
        PendingEventRetry item = PendingEventRetry.create(
                "STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17", "{}",
                "UNCERTAIN", now.minusSeconds(60), firstRequested, "prev");
        for (int i = 0; i < retryCount; i++) {
            item.reschedule(now.minusSeconds(60), "iter");
        }
        return item;
    }

    @Test
    @DisplayName("정상 due 항목 → 원본 payload 그대로 Kafka 재발행")
    void shouldRepublishWhenWithinLimits() {
        PendingEventRetry item = pending(0, now.minusSeconds(3600));
        when(pendingEventRetryQueryService.findDueItems(now)).thenReturn(List.of(item));

        processor.republishDueItems();

        verify(stringKafkaTemplate).send("STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17", "{}");
        verify(failedEventCommandService, never()).escalate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("retryCount가 MAX 이상 → FailedEvent 승격 + PendingEventRetry 삭제")
    void shouldEscalateWhenMaxRetriesExceeded() {
        PendingEventRetry item = pending(PendingEventRetryProcessor.MAX_RETRY_COUNT, now.minusSeconds(3600));
        when(pendingEventRetryQueryService.findDueItems(now)).thenReturn(List.of(item));

        processor.republishDueItems();

        verify(failedEventCommandService).escalate(
                eq("STOCK_PRICE_QUERY"), eq("daily-KOSPI-2026-04-17"), eq("{}"),
                eq(FailedEventCommandService.REASON_RETRY_LIMIT_EXCEEDED), any(String.class), eq(now));
        verify(pendingEventRetryCommandService).delete(item);
        verify(stringKafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("firstRequestedAt이 14일 초과 → 승격")
    void shouldEscalateWhenAgeExceedsMax() {
        Instant old = now.minus(PendingEventRetryProcessor.MAX_AGE).minusSeconds(60);
        PendingEventRetry item = pending(5, old);
        when(pendingEventRetryQueryService.findDueItems(now)).thenReturn(List.of(item));

        processor.republishDueItems();

        verify(failedEventCommandService).escalate(any(), any(), any(), any(), any(), eq(now));
        verify(pendingEventRetryCommandService).delete(item);
    }
}
