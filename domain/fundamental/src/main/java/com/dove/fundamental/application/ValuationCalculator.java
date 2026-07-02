package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;

/**
 * 종가 + PIT 재무로 시가총액과 4비율(PER·PBR·PSR·GP/A)을 계산한다.
 */
public final class ValuationCalculator {

    private ValuationCalculator() {
    }

    /**
     * 주어진 시가총액과 PIT 재무로 4비율을 계산한다. 시총이 없으면(주식수 미확보 등) PER·PBR·PSR은 null.
     * 분모가 없거나 0이면 해당 비율만 null. GP/A는 시총 무관(펀더멘탈)이라 재무만으로 산출한다.
     */
    public static Valuation compute(Long marketCap, StockFundamental f) {
        Double gpa = divide(f.getGrossProfit(), f.getTotalAsset());
        if (marketCap == null) {
            return new Valuation(null, null, null, null, gpa);
        }
        return new Valuation(
                marketCap,
                divide(marketCap, f.getNetIncome()),
                divide(marketCap, f.getTotalEquity()),
                divide(marketCap, f.getRevenue()),
                gpa);
    }

    private static Double divide(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0L) {
            return null;
        }
        return (double) numerator / denominator;
    }
}
