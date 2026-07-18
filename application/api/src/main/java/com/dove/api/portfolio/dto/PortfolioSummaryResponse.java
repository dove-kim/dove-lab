package com.dove.api.portfolio.dto;

/**
 * 포트폴리오 요약 응답 — 대시보드·리포트 헤드라인.
 *
 * @param totalKrw        총 평가자산(현금+평가액, 원화)
 * @param cashKrw         현금 잔액(원화 환산 합계)
 * @param netContribKrw   순납입(입금−출금, 원화)
 * @param growthKrw       증가액(총자산−순납입, 원화)
 * @param evalPnlKrw      보유 평가손익(원화)
 * @param evalPnlPct      보유 평가수익률(%)
 * @param xirrPct         연환산 XIRR(%)
 * @param cashByCurrency  통화별 현금 잔액(원통화, 예: {KRW: …, USD: …})
 */
public record PortfolioSummaryResponse(
        long totalKrw,
        long cashKrw,
        long netContribKrw,
        long growthKrw,
        long evalPnlKrw,
        double evalPnlPct,
        double xirrPct,
        java.util.Map<String, java.math.BigDecimal> cashByCurrency
) {}
