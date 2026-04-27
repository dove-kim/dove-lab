package com.dove.stockbatch.producer;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.eventretry.application.service.PendingEventRetryCommandService;
import com.dove.eventretry.application.service.PendingEventRetryQueryService;
import com.dove.eventretry.domain.entity.PendingEventRetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 이벤트 재시도 대기 큐를 주기적으로 스캔하여 재발행. 한계 초과 건은 수동 검토로 승격. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingEventRetryProcessor {

    static final int MAX_RETRY_COUNT = 20;
    static final Duration MAX_AGE = Duration.ofDays(14);

    private final PendingEventRetryQueryService pendingEventRetryQueryService;
    private final PendingEventRetryCommandService pendingEventRetryCommandService;
    private final FailedEventCommandService failedEventCommandService;
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final Clock clock;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void republishDueItems() {
        Instant now = clock.instant();
        List<PendingEventRetry> due = pendingEventRetryQueryService.findDueItems(now);

        for (PendingEventRetry item : due) {
            if (exceedsLimit(item, now)) {
                failedEventCommandService.escalate(
                        item.getEventType(), item.getEventKey(), item.getEventPayload(),
                        FailedEventCommandService.REASON_RETRY_LIMIT_EXCEEDED,
                        "retryCount=" + item.getRetryCount() + " lastError=" + item.getLastErrorDetail(),
                        now);
                pendingEventRetryCommandService.delete(item);
            } else {
                stringKafkaTemplate.send(item.getEventType(), item.getEventKey(), item.getEventPayload());
                log.info("이벤트 재발행: eventType={} eventKey={} retryCount={}",
                        item.getEventType(), item.getEventKey(), item.getRetryCount());
            }
        }
    }

    private boolean exceedsLimit(PendingEventRetry item, Instant now) {
        if (item.getRetryCount() >= MAX_RETRY_COUNT) {
            return true;
        }
        return Duration.between(item.getFirstRequestedAt(), now).compareTo(MAX_AGE) >= 0;
    }
}
