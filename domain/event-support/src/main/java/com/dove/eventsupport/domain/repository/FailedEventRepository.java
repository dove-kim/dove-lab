package com.dove.eventsupport.domain.repository;

import com.dove.eventsupport.domain.entity.FailedEvent;
import com.dove.eventsupport.domain.enums.FailedEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FailedEvent 리포지토리.
 * 실패 이벤트의 저장 및 상태별 조회를 담당한다.
 */
@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {
}
