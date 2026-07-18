package com.dove.api.portfolio.dto;

import com.dove.portfolio.application.service.PortfolioRoundTrip;

import java.math.BigDecimal;

/**
 * 라운드트립(진입~청산) 성과 응답. 단가는 원통화, 손익은 원화(현재 환율 근사).
 *
 * @param id          순번(응답 내 식별용)
 * @param symbol      종목명
 * @param currency    원통화 코드
 * @param group       계좌명(묶음)
 * @param entry       진입일(ISO)
 * @param exit        청산일(ISO, 미청산이면 null)
 * @param holdingDays 보유일수
 * @param avgNat      평균 매입 단가(원통화)
 * @param exitNat     평균 매도 단가(원통화, 미청산이면 null)
 * @param pnlNat      실현 손익(원통화)
 * @param pnlKrw      실현 손익(원화, 현재 환율 근사)
 * @param pnlPct      수익률(%)
 * @param open        미청산(보유중) 여부
 */
public record PortfolioRoundTripResponse(
        long id,
        String symbol,
        String currency,
        String group,
        String entry,
        String exit,
        long holdingDays,
        BigDecimal avgNat,
        BigDecimal exitNat,
        BigDecimal pnlNat,
        long pnlKrw,
        double pnlPct,
        boolean open
) {
    /**
     * 도메인 라운드트립을 원화 환산·계좌명과 함께 응답으로 변환한다.
     */
    public static PortfolioRoundTripResponse of(long id, PortfolioRoundTrip t, String group, long pnlKrw) {
        return new PortfolioRoundTripResponse(id, t.symbol(), t.currency(), group,
                t.entryDate().toString(), t.exitDate() != null ? t.exitDate().toString() : null,
                t.holdingDays(), t.avgNat(), t.exitNat(), t.pnlNat(), pnlKrw, t.pnlPct(), t.open());
    }
}
