package com.dove.api.ops.custommetric.dto;

import com.dove.stock.domain.enums.PriceType;
import jakarta.validation.constraints.NotBlank;

/**
 * 커스텀 지표 미리보기 요청(초안 스펙을 최근 구간에 대해 시험 계산).
 *
 * @param spec      계산식 DSL(JSON)
 * @param priceType 피처 주가 유형(null=RAW)
 */
public record MetricPreviewRequest(@NotBlank String spec, PriceType priceType) {
}
