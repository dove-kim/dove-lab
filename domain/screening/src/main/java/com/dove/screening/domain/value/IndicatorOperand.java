package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;

/**
 * 지표 값 피연산자.
 *
 * @param type   지표 종류
 * @param offset 거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record IndicatorOperand(IndicatorType type, int offset) implements FilterOperand {
}
