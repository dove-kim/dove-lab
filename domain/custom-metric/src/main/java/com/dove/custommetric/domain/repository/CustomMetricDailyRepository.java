package com.dove.custommetric.domain.repository;

import com.dove.custommetric.domain.entity.CustomMetricDaily;
import com.dove.custommetric.domain.entity.CustomMetricDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 커스텀 지표 계산값(CUSTOM_METRIC_DAILY) 저장소.
 */
@Repository
public interface CustomMetricDailyRepository extends JpaRepository<CustomMetricDaily, CustomMetricDailyId> {

    /**
     * 지표의 저장된 값 전체를 삭제한다(재계산·삭제 시).
     */
    void deleteByIdMetricId(Long metricId);
}
