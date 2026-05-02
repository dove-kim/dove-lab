package com.dove.stockconsumer.listener;

import com.dove.distributedlock.DistributedLockProperties;
import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.application.dto.IndicatorCalcRequest;
import com.dove.technicalindicator.application.service.IndicatorCursorAdvanceService;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorCalcConsumerTest {

    @Mock private IndicatorCursorAdvanceService advanceService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock lock;
    @Mock private FailedEventCommandService failedEventCommandService;
    @Mock private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC);
    private IndicatorCalcConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new IndicatorCalcConsumer(
                advanceService, kafkaTemplate, objectMapper,
                redissonClient, new DistributedLockProperties(),
                failedEventCommandService, clock);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
    }

    private ConsumerRecord<String, IndicatorCalcRequest> record(IndicatorCalcRequest req) {
        return new ConsumerRecord<>(IndicatorCalcRequest.TOPIC, 0, 0, req.messageKey(), req);
    }

    @Test
    @DisplayName("advance 성공 → 다음 날짜 포함한 체이닝 이벤트 발행 + ack")
    void shouldChainWithNextDateWhenAdvanced() throws Exception {
        LocalDate nextDate = LocalDate.of(2024, 1, 8);
        IndicatorCalcRequest req = new IndicatorCalcRequest(
                MarketType.KOSPI, "005930", IndicatorType.SMA_5, LocalDate.of(2024, 1, 5));
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        given(advanceService.advance(eq(MarketType.KOSPI), eq("005930"), eq(IndicatorType.SMA_5), any()))
                .willReturn(Optional.of(nextDate));

        consumer.onIndicatorCalc(record(req), acknowledgment);

        ArgumentCaptor<IndicatorCalcRequest> captor = ArgumentCaptor.forClass(IndicatorCalcRequest.class);
        verify(kafkaTemplate).send(eq(IndicatorCalcRequest.TOPIC), eq("KOSPI-005930-SMA_5"), captor.capture());
        assertThat(captor.getValue().getInsertedDate()).isEqualTo(nextDate);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("advance empty → 체이닝 없음 + ack")
    void shouldNotChainWhenAdvanceReturnsEmpty() throws Exception {
        IndicatorCalcRequest req = new IndicatorCalcRequest(MarketType.KOSPI, "005930", IndicatorType.SMA_5);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        given(advanceService.advance(eq(MarketType.KOSPI), eq("005930"), eq(IndicatorType.SMA_5), any()))
                .willReturn(Optional.empty());

        consumer.onIndicatorCalc(record(req), acknowledgment);

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("락 획득 실패 → skip + ack")
    void shouldSkipWhenLockNotAcquired() throws Exception {
        IndicatorCalcRequest req = new IndicatorCalcRequest(MarketType.KOSPI, "005930", IndicatorType.SMA_5);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        consumer.onIndicatorCalc(record(req), acknowledgment);

        verify(advanceService, never()).advance(any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("advance 예외 → FailedEvent escalate + ack")
    void shouldEscalateAndAckOnServiceException() throws Exception {
        IndicatorCalcRequest req = new IndicatorCalcRequest(MarketType.KOSPI, "005930", IndicatorType.SMA_5);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        given(advanceService.advance(any(), any(), any(), any())).willThrow(new RuntimeException("db error"));

        consumer.onIndicatorCalc(record(req), acknowledgment);

        verify(failedEventCommandService).escalate(
                eq(IndicatorCalcRequest.TOPIC), eq("KOSPI-005930-SMA_5"),
                anyString(), anyString(), anyString(), eq(clock.instant()));
        verify(acknowledgment).acknowledge();
    }
}
