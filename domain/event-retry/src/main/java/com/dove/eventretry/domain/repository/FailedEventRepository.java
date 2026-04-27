package com.dove.eventretry.domain.repository;

import com.dove.eventretry.domain.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);
}
