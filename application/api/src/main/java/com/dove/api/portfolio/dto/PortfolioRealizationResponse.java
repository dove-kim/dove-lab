package com.dove.api.portfolio.dto;

import com.dove.portfolio.application.service.PortfolioRealization;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 매도 한 건의 실현 손익 응답. 기간별 집계는 이 날짜를 기준으로 한다.
 *
 * @param date     매도 일자(ISO)
 * @param quantity 매도 수량
 * @param pnlNat   실현 손익(원통화)
 * @param pnlKrw   실현 손익(원화, 현재 환율 근사)
 * @param costKrw  판 만큼의 매입 원가(원화, 현재 환율 근사) — 기간별 수익률의 분모
 * @param pnlPct   수익률(%, 판 만큼의 원가 기준)
 */
public record PortfolioRealizationResponse(
        String date,
        BigDecimal quantity,
        BigDecimal pnlNat,
        long pnlKrw,
        long costKrw,
        double pnlPct
) {
    /**
     * 도메인 실현 이벤트를 원화 환산과 함께 응답으로 변환한다.
     */
    public static PortfolioRealizationResponse of(PortfolioRealization r, BigDecimal fxRate) {
        long pnlKrw = r.pnl().multiply(fxRate).setScale(0, RoundingMode.HALF_UP).longValue();
        long costKrw = r.costBasis().multiply(fxRate).setScale(0, RoundingMode.HALF_UP).longValue();
        double pnlPct = r.costBasis().signum() > 0
                ? Math.round(r.pnl().multiply(BigDecimal.valueOf(100))
                        .divide(r.costBasis(), 6, RoundingMode.HALF_UP).doubleValue() * 100.0) / 100.0
                : 0.0;
        return new PortfolioRealizationResponse(r.sellDate().toString(), r.quantity(), r.pnl(), pnlKrw, costKrw, pnlPct);
    }
}
