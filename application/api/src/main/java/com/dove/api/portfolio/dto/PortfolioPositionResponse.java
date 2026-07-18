package com.dove.api.portfolio.dto;

import java.math.BigDecimal;

/**
 * 포트폴리오 보유 포지션 응답. 단가는 원통화, 평가·손익은 원화.
 *
 * @param symbol      종목명
 * @param account     계좌명
 * @param currency    원통화 코드
 * @param tag         자유 태그(없으면 null)
 * @param quantity    보유 수량
 * @param avgPriceNat 평균 매입 단가(원통화)
 * @param curPriceNat 현재가(원통화)
 * @param evalKrw     평가액(원화)
 * @param pnlKrw      평가손익(원화)
 * @param pnlPct            수익률(%)
 * @param weightPct         포트폴리오 내 비중(%)
 * @param holdingId         연동된 보유 ID(미연동이면 null — 배당률 설정 불가)
 * @param annualDividendPct 연 배당수익률(%, 없으면 null)
 * @param dividendTracked   배당 추적 대상 여부
 */
public record PortfolioPositionResponse(
        String symbol,
        String account,
        String currency,
        String tag,
        BigDecimal quantity,
        BigDecimal avgPriceNat,
        BigDecimal curPriceNat,
        long evalKrw,
        long pnlKrw,
        double pnlPct,
        double weightPct,
        Long holdingId,
        Double annualDividendPct,
        boolean dividendTracked
) {}
