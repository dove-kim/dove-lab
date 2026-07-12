package com.dove.custommetric.domain.spec;

/**
 * 시차 — 시계열을 periods만큼 뒤로 민다(과거값 참조). 앞쪽 periods개는 NaN.
 *
 * @param input   입력 시계열
 * @param periods 시차(거래일 수)
 */
public record LagNode(MetricNode input, int periods) implements MetricNode {
}
