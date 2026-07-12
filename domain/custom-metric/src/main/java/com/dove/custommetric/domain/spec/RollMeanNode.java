package com.dove.custommetric.domain.spec;

/**
 * 이동평균(SMA) — 시계열의 window일 롤링 평균. 표본이 minPeriods 미만인 구간은 NaN.
 *
 * @param input      입력 시계열
 * @param window     창 길이
 * @param minPeriods 값을 내기 위한 최소 표본 수
 */
public record RollMeanNode(MetricNode input, int window, int minPeriods) implements MetricNode {
}
