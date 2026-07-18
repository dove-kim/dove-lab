package com.dove.api.portfolio.service;

import java.math.BigDecimal;

/**
 * 현재가·환율까지 반영해 평가액을 계산한 포지션 중간 결과(비중 계산 전).
 *
 * @param account     계좌명
 * @param symbol      종목명
 * @param currency    원통화 코드
 * @param quantity    보유 수량
 * @param avgPriceNat 평균 매입 단가(원통화)
 * @param curPriceNat 현재가(원통화)
 * @param evalKrw            평가액(원화)
 * @param investedKrw        매입 원가(원화)
 * @param holdingId          연동된 보유 ID(미연동이면 null — 배당률 설정 불가)
 * @param annualDividendPct  연 배당수익률(%, 없으면 null)
 * @param dividendTracked    배당 추적 대상 여부
 */
record ResolvedPosition(
        String account,
        String symbol,
        String currency,
        BigDecimal quantity,
        BigDecimal avgPriceNat,
        BigDecimal curPriceNat,
        long evalKrw,
        long investedKrw,
        Long holdingId,
        Double annualDividendPct,
        boolean dividendTracked
) {}
