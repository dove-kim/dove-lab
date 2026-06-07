package com.dove.systemevent.domain.repository;

import com.dove.systemevent.domain.entity.SystemEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 시스템 이벤트 영속성 저장소.
 */
public interface SystemEventRepository extends JpaRepository<SystemEvent, Long> {

    Page<SystemEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
