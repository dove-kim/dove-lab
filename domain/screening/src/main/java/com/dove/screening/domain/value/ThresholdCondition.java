package com.dove.screening.domain.value;

/**
 * 피연산자를 상수와 비교하는 조건.
 *
 * @param operand  비교 대상 값
 * @param operator 비교 연산자
 * @param value    기준 상수
 */
public record ThresholdCondition(FilterOperand operand, FilterOperator operator, double value) implements FilterNode {
}
