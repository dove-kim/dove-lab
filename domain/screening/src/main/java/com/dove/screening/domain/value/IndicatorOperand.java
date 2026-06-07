package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;

/**
 * 지표 값 피연산자.
 *
 * @param type 지표 종류
 */
public record IndicatorOperand(IndicatorType type) implements FilterOperand {
}
