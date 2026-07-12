package com.dove.custommetric.application.service;

import com.dove.custommetric.domain.entity.CustomMetricDef;
import com.dove.custommetric.domain.entity.MetricShape;
import com.dove.custommetric.domain.repository.CustomMetricDefRepository;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.MetricSpecParser;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 커스텀 지표 정의(CUSTOM_METRIC_DEF)를 관리하는 서비스 — CRUD·활성·진행 상태.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomMetricDefService {

    private final CustomMetricDefRepository repository;

    /**
     * 정의를 생성한다. 스펙 JSON이 파싱 불가면 거부한다.
     *
     * @throws IllegalArgumentException 스펙 JSON이 잘못된 경우
     */
    public CustomMetricDef create(String name, String description, MetricShape shape,
                                  String spec, PriceType priceType, String createdBy) {
        MetricSpecParser.parse(spec); // 검증
        return repository.save(CustomMetricDef.create(name, description, shape, spec, priceType, createdBy));
    }

    /**
     * 정의를 갱신한다. 스펙 JSON이 파싱 불가면 거부한다.
     *
     * @throws IllegalArgumentException 정의가 없거나 스펙이 잘못된 경우
     */
    public void update(Long id, String name, String description, String spec, PriceType priceType) {
        MetricSpecParser.parse(spec);
        CustomMetricDef def = getOrThrow(id);
        def.update(name, description, spec, priceType);
    }

    /**
     * 활성 여부를 변경한다.
     */
    public void updateActive(Long id, boolean active) {
        getOrThrow(id).updateActive(active);
    }

    /**
     * 진행 상태를 초기화한다(다음 배치가 처음부터 재계산).
     */
    public void resetProgress(Long id) {
        getOrThrow(id).resetProgress();
    }

    /**
     * 정의를 삭제한다.
     *
     * @throws IllegalArgumentException 정의가 없는 경우
     */
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    /**
     * 계산 완료 지점을 기록한다.
     */
    public void recordComputed(Long id, LocalDate lastDate) {
        getOrThrow(id).recordComputed(lastDate);
    }

    /**
     * 계산 오류를 기록한다.
     */
    public void recordError(Long id, String message) {
        repository.findById(id).ifPresent(d -> d.recordError(message));
    }

    /**
     * 활성 정의 전체를 반환한다(야간 계산 대상).
     */
    @Transactional(readOnly = true)
    public List<CustomMetricDef> findActive() {
        return repository.findByActiveTrue();
    }

    /**
     * 전체 정의를 반환한다(관리 화면).
     */
    @Transactional(readOnly = true)
    public List<CustomMetricDef> findAll() {
        return repository.findAll();
    }

    /**
     * ID로 정의를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<CustomMetricDef> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 스펙 JSON을 계산식 트리로 파싱한다(파싱·jackson을 이 모듈 안에 격리).
     *
     * @throws IllegalArgumentException 스펙이 잘못된 경우
     */
    public MetricSpec parseSpec(String spec) {
        return MetricSpecParser.parse(spec);
    }

    private CustomMetricDef getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CUSTOM_METRIC_NOT_FOUND"));
    }
}
