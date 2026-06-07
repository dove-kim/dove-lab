package com.dove.screening.domain.value;

/**
 * 피연산자가 범위 안에 드는지 보는 조건.
 *
 * @param operand 대상 값
 * @param range   허용 범위
 */
public record RangeCondition(FilterOperand operand, FilterRange range) implements FilterNode {
}
