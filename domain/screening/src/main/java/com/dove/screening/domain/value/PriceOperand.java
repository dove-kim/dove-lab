package com.dove.screening.domain.value;

/**
 * 가격 필드 피연산자.
 *
 * @param field 가격 필드
 */
public record PriceOperand(FilterPriceField field) implements FilterOperand {
}
