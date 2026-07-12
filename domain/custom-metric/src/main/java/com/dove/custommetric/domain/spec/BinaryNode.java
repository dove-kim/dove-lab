package com.dove.custommetric.domain.spec;

/**
 * 이항 연산 노드 — 두 시계열(또는 시계열·상수)의 원소별 연산. 비교 연산은 1.0/0.0을 낸다.
 *
 * @param op    연산자
 * @param left  좌변
 * @param right 우변
 */
public record BinaryNode(BinaryOp op, MetricNode left, MetricNode right) implements MetricNode {
}
