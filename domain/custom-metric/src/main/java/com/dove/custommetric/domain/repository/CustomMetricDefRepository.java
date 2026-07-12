package com.dove.custommetric.domain.repository;

import com.dove.custommetric.domain.entity.CustomMetricDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 커스텀 지표 정의(CUSTOM_METRIC_DEF) 저장소.
 */
@Repository
public interface CustomMetricDefRepository extends JpaRepository<CustomMetricDef, Long> {

    /**
     * 활성 지표 정의 전체를 반환한다(야간 계산 대상).
     */
    List<CustomMetricDef> findByActiveTrue();

    /**
     * 이름으로 지표 정의를 찾는다.
     */
    Optional<CustomMetricDef> findByName(String name);
}
