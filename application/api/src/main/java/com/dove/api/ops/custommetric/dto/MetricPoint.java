package com.dove.api.ops.custommetric.dto;

/**
 * 미리보기 시계열의 한 점.
 *
 * @param date  거래일(yyyy-MM-dd)
 * @param value 계산값(미확정이면 null)
 */
public record MetricPoint(String date, Double value) {
}
