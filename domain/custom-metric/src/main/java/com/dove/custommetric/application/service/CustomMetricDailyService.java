package com.dove.custommetric.application.service;

import com.dove.custommetric.domain.entity.CustomMetricDaily;
import com.dove.custommetric.domain.entity.CustomMetricDailyId;
import com.dove.custommetric.domain.repository.CustomMetricDailyRepository;
import com.dove.custommetric.infrastructure.repository.CustomMetricDailyQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 커스텀 지표 계산값(CUSTOM_METRIC_DAILY)을 저장·조회·삭제하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomMetricDailyService {

    private final CustomMetricDailyRepository repository;
    private final CustomMetricDailyQuerySupport querySupport;

    /**
     * 계산값 행들을 저장(upsert)한다. 비어있으면 아무 것도 하지 않는다.
     */
    public void saveAll(List<CustomMetricDaily> rows) {
        repository.saveAll(rows);
    }

    /**
     * 지표의 저장된 값 전체를 삭제한다(재계산·정의 삭제 시).
     */
    public void deleteByMetric(Long metricId) {
        repository.deleteByIdMetricId(metricId);
    }

    /**
     * 지표·거래일의 계산값을 반환한다. 없으면 비어있다.
     */
    @Transactional(readOnly = true)
    public Optional<Double> findValue(Long metricId, LocalDate date) {
        return repository.findById(new CustomMetricDailyId(metricId, date)).map(CustomMetricDaily::getValue);
    }

    /**
     * 지표의 저장값을 거래일 범위(양끝 포함, 오름차순)로 조회한다. from/to가 null이면 그 방향은 무제한.
     */
    @Transactional(readOnly = true)
    public List<CustomMetricDaily> findByMetricAndDateRange(Long metricId, LocalDate from, LocalDate to) {
        return querySupport.findRange(metricId, from, to);
    }
}
