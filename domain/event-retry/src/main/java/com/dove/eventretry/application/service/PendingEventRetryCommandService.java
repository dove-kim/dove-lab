package com.dove.eventretry.application.service;

import com.dove.eventretry.domain.entity.PendingEventRetry;
import com.dove.eventretry.domain.repository.PendingEventRetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 이벤트 재시도 큐 변경 서비스. */
@Service
@RequiredArgsConstructor
public class PendingEventRetryCommandService {

    private final PendingEventRetryRepository pendingEventRetryRepository;

    @Transactional
    public void enqueueOrUpdate(String eventType, String eventKey, String eventPayload,
                                String reason, Instant nextRetryAt,
                                Instant now, String errorDetail) {
        pendingEventRetryRepository.findByEventTypeAndEventKey(eventType, eventKey)
                .ifPresentOrElse(
                        existing -> existing.reschedule(nextRetryAt, errorDetail),
                        () -> pendingEventRetryRepository.save(PendingEventRetry.create(
                                eventType, eventKey, eventPayload, reason,
                                nextRetryAt, now, errorDetail))
                );
    }

    @Transactional
    public void deleteByEvent(String eventType, String eventKey) {
        pendingEventRetryRepository.findByEventTypeAndEventKey(eventType, eventKey)
                .ifPresent(pendingEventRetryRepository::delete);
    }

    @Transactional
    public void delete(PendingEventRetry item) {
        pendingEventRetryRepository.delete(item);
    }
}
