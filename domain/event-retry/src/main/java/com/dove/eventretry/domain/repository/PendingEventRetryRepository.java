package com.dove.eventretry.domain.repository;

import com.dove.eventretry.domain.entity.PendingEventRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingEventRetryRepository extends JpaRepository<PendingEventRetry, Long> {

    List<PendingEventRetry> findAllByNextRetryAtLessThanEqualOrderByNextRetryAtAsc(Instant threshold);

    Optional<PendingEventRetry> findByEventTypeAndEventKey(String eventType, String eventKey);
}
