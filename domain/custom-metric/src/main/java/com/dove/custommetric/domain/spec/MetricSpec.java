package com.dove.custommetric.domain.spec;

import java.util.Map;

/**
 * 커스텀 지표 계산식 — 명명 중간값(let)과 최종 표현식(root) 트리.
 *
 * @param lets 명명 중간값 (이름 → 노드). RefNode가 참조
 * @param root 최종 표현식
 */
public record MetricSpec(Map<String, MetricNode> lets, MetricNode root) {
}
