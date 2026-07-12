package com.dove.api.ops.custommetric.dto;

import com.dove.custommetric.domain.entity.CustomMetricDef;

/**
 * 필터 빌더용 커스텀 지표 요약(선택 목록).
 *
 * @param id    지표 ID
 * @param name  이름
 * @param shape 출력 모양
 */
public record CustomMetricSummary(Long id, String name, String shape) {

    /**
     * 도메인 정의를 요약 DTO로 변환한다.
     */
    public static CustomMetricSummary from(CustomMetricDef d) {
        return new CustomMetricSummary(d.getId(), d.getName(), d.getShape().name());
    }
}
