package com.dove.screening.domain.value;

/**
 * 두 피연산자를 비교하는 조건.
 *
 * @param left     좌변 값
 * @param operator 비교 연산자
 * @param right    우변 값
 */
public record ComparisonCondition(FilterOperand left, FilterOperator operator, FilterOperand right) implements FilterNode {
}
