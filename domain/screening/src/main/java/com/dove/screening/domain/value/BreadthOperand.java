package com.dove.screening.domain.value;

/**
 * 당일 상승비율 피연산자 — universe 단일 스칼라(거래소·가격유형·거래일당 1값).
 *
 * @param offset 거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record BreadthOperand(int offset) implements FilterOperand {
}
