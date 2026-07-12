package com.dove.screening.domain.value;

/**
 * 거래대금 피연산자.
 *
 * @param offset 거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record TurnoverOperand(int offset) implements FilterOperand {
}
