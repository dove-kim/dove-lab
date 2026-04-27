package com.dove.eventretry.application.service;

import com.dove.eventretry.domain.entity.PendingEventRetry;
import com.dove.eventretry.domain.repository.PendingEventRetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** 이벤트 재시도 큐 조회 전용 서비스. */
@Service
@RequiredArgsConstructor
public class PendingEventRetryQueryService {

    private final PendingEventRetryRepository pendingEventRetryRepository;

    @Transactional(readOnly = true)
    public List<PendingEventRetry> findDueItems(Instant now) {
        return pendingEventRetryRepository.findAllByNextRetryAtLessThanEqualOrderByNextRetryAtAsc(now);
    }
}
