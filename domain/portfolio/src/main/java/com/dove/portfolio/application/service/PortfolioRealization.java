package com.dove.portfolio.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매도 한 건에서 확정된 실현 손익 — 기간별 집계의 귀속 단위. 금액은 거래 통화(원통화) 기준.
 *
 * @param sellDate  매도 일자
 * @param quantity  매도 수량
 * @param costBasis 판 만큼의 매입 원가
 * @param proceeds  수수료를 뺀 매도 대금
 * @param pnl       실현 손익(proceeds − costBasis)
 */
public record PortfolioRealization(
        LocalDate sellDate,
        BigDecimal quantity,
        BigDecimal costBasis,
        BigDecimal proceeds,
        BigDecimal pnl
) {}
