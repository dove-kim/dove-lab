package com.dove.screening.domain.value;

/**
 * 수치형 범위 조건 — 자본금·액면가·상장주식수 등을 이상/이하로 거른다.
 *
 * @param field {@link com.dove.stock.domain.enums.NumericField} 이름
 * @param min   하한(이상, 포함). null이면 하한 없음.
 * @param max   상한(이하, 포함). null이면 상한 없음.
 */
public record NumericCondition(
        String field,
        Long min,
        Long max
) {
}
