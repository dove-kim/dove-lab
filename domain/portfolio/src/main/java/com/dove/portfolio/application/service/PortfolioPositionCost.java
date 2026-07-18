package com.dove.portfolio.application.service;

import java.math.BigDecimal;

/**
 * 한 (계좌, 종목)의 원가 기준 포지션 상태 — 거래를 접어 파생한 결과(현재가·평가액 제외). 금액은 거래 통화(원통화) 기준.
 *
 * @param accountId      계좌 ID
 * @param symbol         종목명
 * @param currency       원통화 코드
 * @param quantity       보유 수량(원통화 종목 단위)
 * @param avgPriceNat    평균 매입 단가(원통화)
 * @param investedNat    현재 보유분의 매입 원가(원통화, 수수료 포함)
 * @param realizedPnlNat 실현 손익(원통화, 부분 매도 누적)
 */
public record PortfolioPositionCost(
        Long accountId,
        String symbol,
        String currency,
        BigDecimal quantity,
        BigDecimal avgPriceNat,
        BigDecimal investedNat,
        BigDecimal realizedPnlNat
) {}
