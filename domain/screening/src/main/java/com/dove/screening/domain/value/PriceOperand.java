package com.dove.screening.domain.value;

/**
 * 가격 필드 피연산자.
 *
 * @param field  가격 필드
 * @param offset 거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record PriceOperand(FilterPriceField field, int offset) implements FilterOperand {
}
