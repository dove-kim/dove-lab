package com.dove.eventretry.application.service;

import com.dove.eventretry.domain.entity.FailedEvent;
import com.dove.eventretry.domain.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 수동 검토 큐 변경 서비스 (DLQ). */
@Service
@RequiredArgsConstructor
public class FailedEventCommandService {

    /** 재시도 한계(횟수/기간) 초과로 승격한 건. PendingEventRetryProcessor가 사용. */
    public static final String REASON_RETRY_LIMIT_EXCEEDED = "RETRY_LIMIT_EXCEEDED";

    private final FailedEventRepository failedEventRepository;

    @Transactional
    public void escalate(String eventType, String eventKey, String eventPayload,
                         String reason, String detail, Instant now) {
        if (!failedEventRepository.existsByEventTypeAndEventKey(eventType, eventKey)) {
            failedEventRepository.save(FailedEvent.escalate(
                    eventType, eventKey, eventPayload, reason, detail, now));
        }
    }
}
