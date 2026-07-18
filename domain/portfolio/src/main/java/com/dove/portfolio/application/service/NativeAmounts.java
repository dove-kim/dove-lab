package com.dove.portfolio.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원통화 금액 계산 보조.
 */
final class NativeAmounts {

    private static final int NAT_SCALE = 8;

    private NativeAmounts() {
    }

    /**
     * value × remaining ÷ quantity — 원통화 원가의 잔여 비례분.
     */
    static BigDecimal proportion(BigDecimal value, BigDecimal remaining, BigDecimal quantity) {
        return value.multiply(remaining).divide(quantity, NAT_SCALE, RoundingMode.HALF_UP);
    }
}
