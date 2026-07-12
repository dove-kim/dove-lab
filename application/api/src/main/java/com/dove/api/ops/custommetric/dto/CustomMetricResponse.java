package com.dove.api.ops.custommetric.dto;

import com.dove.custommetric.domain.entity.CustomMetricDef;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 커스텀 지표 정의 응답(관리 화면).
 *
 * @param id               지표 ID
 * @param name             이름
 * @param description      설명
 * @param shape            출력 모양
 * @param spec             계산식 DSL(JSON)
 * @param priceType        피처 주가 유형
 * @param active           활성 여부
 * @param lastComputedDate 마지막 계산 거래일(null=미계산)
 * @param lastError        마지막 계산 오류(null=정상)
 * @param createdBy        생성자
 * @param createdAt        생성 일시
 * @param updatedAt        수정 일시
 */
public record CustomMetricResponse(
        Long id,
        String name,
        String description,
        String shape,
        String spec,
        String priceType,
        boolean active,
        LocalDate lastComputedDate,
        String lastError,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /**
     * 도메인 정의를 응답 DTO로 변환한다.
     */
    public static CustomMetricResponse from(CustomMetricDef d) {
        return new CustomMetricResponse(d.getId(), d.getName(), d.getDescription(), d.getShape().name(),
                d.getSpec(), d.getPriceType().name(), d.isActive(), d.getLastComputedDate(), d.getLastError(),
                d.getCreatedBy(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
