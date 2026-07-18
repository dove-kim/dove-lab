package com.dove.portfolio.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 라운드트립 fold 중 한 (계좌, 종목)의 진행 사이클 상태를 담는 가변 홀더. 금액은 거래 통화(원통화) 기준.
 */
class RoundTripState {

    final Long accountId;
    final String symbol;
    final String currency;

    BigDecimal quantity = BigDecimal.ZERO;
    BigDecimal investedNat = BigDecimal.ZERO;

    boolean cycleOpen = false;
    LocalDate entryDate;
    BigDecimal buyQty = BigDecimal.ZERO;
    BigDecimal buyPxQty = BigDecimal.ZERO;
    BigDecimal sellQty = BigDecimal.ZERO;
    BigDecimal sellPxQty = BigDecimal.ZERO;
    BigDecimal costBasis = BigDecimal.ZERO;
    BigDecimal realizedPnl = BigDecimal.ZERO;

    RoundTripState(Long accountId, String symbol, String currency) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.currency = currency;
    }

    /**
     * 새 사이클을 시작한다(진입일 기록, 누적값 초기화).
     */
    void openCycle(LocalDate date) {
        cycleOpen = true;
        entryDate = date;
        buyQty = BigDecimal.ZERO;
        buyPxQty = BigDecimal.ZERO;
        sellQty = BigDecimal.ZERO;
        sellPxQty = BigDecimal.ZERO;
        costBasis = BigDecimal.ZERO;
        realizedPnl = BigDecimal.ZERO;
        investedNat = BigDecimal.ZERO;
    }
}
