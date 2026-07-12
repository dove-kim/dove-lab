package com.dove.custommetric.domain.spec;

/**
 * 명명 중간값 참조 — 스펙의 let에 정의된 노드를 재사용한다(중복 계산 방지).
 *
 * @param name let 이름
 */
public record RefNode(String name) implements MetricNode {
}
