package com.dove.portfolio.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 한 라운드트립(진입~청산 한 사이클)의 성과 — 거래를 접어 파생한 결과. 금액은 거래 통화(원통화) 기준.
 *
 * @param accountId   계좌 ID
 * @param symbol      종목명
 * @param currency    원통화 코드
 * @param entryDate   진입일(사이클 첫 매수)
 * @param exitDate    청산일(전량 매도, 미청산이면 null)
 * @param holdingDays 보유일수(진입~청산, 미청산이면 진입~기준일)
 * @param avgNat      사이클 평균 매입 단가(원통화)
 * @param exitNat     사이클 평균 매도 단가(원통화, 미청산이면 null)
 * @param pnlNat      실현 손익(원통화)
 * @param pnlPct      수익률(%, 원통화 기준 — 환율 무관)
 * @param open        미청산(보유중) 여부
 */
public record PortfolioRoundTrip(
        Long accountId,
        String symbol,
        String currency,
        LocalDate entryDate,
        LocalDate exitDate,
        long holdingDays,
        BigDecimal avgNat,
        BigDecimal exitNat,
        BigDecimal pnlNat,
        double pnlPct,
        boolean open
) {}
