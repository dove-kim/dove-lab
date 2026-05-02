package com.dove.stockconsumer.listener;

import com.dove.distributedlock.DistributedLockProperties;
import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.technicalindicator.application.dto.IndicatorCalcRequest;
import com.dove.technicalindicator.application.service.IndicatorCursorAdvanceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorCalcConsumer {

    private final IndicatorCursorAdvanceService advanceService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final DistributedLockProperties lockProperties;
    private final FailedEventCommandService failedEventCommandService;
    private final Clock clock;

    @KafkaListener(
            groupId = "indicatorCalc-1",
            topics = IndicatorCalcRequest.TOPIC,
            concurrency = "4",
            containerFactory = "indicatorCalcFactory"
    )
    public void onIndicatorCalc(ConsumerRecord<String, IndicatorCalcRequest> data, Acknowledgment acknowledgment) {
        try {
            processWithLock(data.value());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("지표 커서 전진 실패: key={}, error={}", data.key(), e.getMessage(), e);
            escalate(data, e);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private void processWithLock(IndicatorCalcRequest req) throws InterruptedException {
        String lockKey = "lock:indicator-calc:"
                + req.getMarketType() + ":" + req.getStockCode() + ":" + req.getIndicatorType();
        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.tryLock(0, lockProperties.getLeaseTime(), TimeUnit.SECONDS)) {
            return;
        }
        try {
            advanceService.advance(
                    req.getMarketType(), req.getStockCode(),
                    req.getIndicatorType(), req.getInsertedDate())
                    .ifPresent(nextDate -> {
                        IndicatorCalcRequest next = new IndicatorCalcRequest(
                                req.getMarketType(), req.getStockCode(), req.getIndicatorType(), nextDate);
                        kafkaTemplate.send(IndicatorCalcRequest.TOPIC, next.messageKey(), next);
                    });
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private void escalate(ConsumerRecord<String, IndicatorCalcRequest> data, Exception e) {
        try {
            String payload = objectMapper.writeValueAsString(data.value());
            failedEventCommandService.escalate(
                    data.topic(), data.key(), payload,
                    "INDICATOR_CALC_ERROR", e.getMessage(), clock.instant());
        } catch (JsonProcessingException | RuntimeException ex) {
            log.error("FailedEvent 기록 실패: key={}, error={}", data.key(), ex.getMessage());
        }
    }
}

