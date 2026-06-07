package com.dove.screening.domain.value;

/**
 * 검색식 조건이 비교하는 값의 출처.
 */
public sealed interface FilterOperand
        permits IndicatorOperand, PriceOperand, VolumeOperand {
}
