package com.dove.screening.domain.value;

import com.dove.indicator.domain.rank.enums.RankType;

/**
 * 횡단면 percentile 순위 피연산자.
 *
 * @param type   순위 종류
 * @param offset 거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record RankOperand(RankType type, int offset) implements FilterOperand {
}
