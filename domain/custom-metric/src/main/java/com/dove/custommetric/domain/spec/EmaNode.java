package com.dove.custommetric.domain.spec;

/**
 * 지수이동평균(EMA) — 평활계수 2/(window+1). 첫 유효값은 SMA(window)로 시드한다.
 *
 * @param input  입력 시계열
 * @param window 창 길이
 */
public record EmaNode(MetricNode input, int window) implements MetricNode {
}
