package com.dove.api.ops.custommetric.dto;

import com.dove.custommetric.domain.entity.MetricShape;
import com.dove.stock.domain.enums.PriceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 커스텀 지표 생성·수정 요청.
 *
 * @param name        지표 이름(고유)
 * @param description 설명
 * @param shape       출력 모양(SERIES/PANEL)
 * @param spec        계산식 DSL(JSON)
 * @param priceType   피처 주가 유형(RAW/ADJUSTED, null=RAW)
 */
public record CustomMetricRequest(
        @NotBlank String name,
        String description,
        @NotNull MetricShape shape,
        @NotBlank String spec,
        PriceType priceType) {
}
