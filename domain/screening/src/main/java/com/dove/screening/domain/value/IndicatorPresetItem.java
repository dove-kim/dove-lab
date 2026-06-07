package com.dove.screening.domain.value;

/**
 * 지표 프리셋의 개별 지표 설정.
 *
 * @param type      IndicatorType 이름 (문자열)
 * @param enabled   표시 여부
 * @param color     선 색상 hex
 * @param lineWidth 선 굵기
 */
public record IndicatorPresetItem(
        String type,
        boolean enabled,
        String color,
        double lineWidth
) {}
