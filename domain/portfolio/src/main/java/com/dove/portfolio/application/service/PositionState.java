package com.dove.portfolio.application.service;

import java.math.BigDecimal;

/**
 * fold 중 한 (계좌, 종목)의 누적 상태를 담는 가변 홀더. 금액은 거래 통화(원통화) 기준.
 */
class PositionState {

    final Long accountId;
    final String symbol;
    final String currency;
    BigDecimal quantity = BigDecimal.ZERO;
    BigDecimal costNat = BigDecimal.ZERO;
    BigDecimal investedNat = BigDecimal.ZERO;
    BigDecimal realizedPnlNat = BigDecimal.ZERO;

    PositionState(Long accountId, String symbol, String currency) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.currency = currency;
    }
}
