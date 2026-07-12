package com.dove.custommetric.domain.spec;

/**
 * 누적 지수 — ∏(1+x). 일간 수익률 시계열을 누적지수로 만든다(중간 NaN은 0으로 간주).
 *
 * @param input 일간 수익률 시계열
 */
public record CumProd1pNode(MetricNode input) implements MetricNode {
}
