package com.dove.portfolio.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 날짜별 현금흐름의 연환산 내부수익률(XIRR)을 이분법으로 구하는 순수 계산기.
 */
public final class Xirr {

    private static final double LOW = -0.9999;
    private static final double HIGH = 100.0;
    private static final int ITERATIONS = 200;

    private Xirr() {
    }

    /**
     * 외부 현금흐름 + 평가일의 현재가치로 연 XIRR(%)을 구한다. 해가 없으면 빈 값.
     *
     * @param flows        외부 현금흐름(투자자 관점 부호)
     * @param valuationOn  평가 기준일
     * @param currentValue 평가일의 현재 자산가치(원화, 투자자에게 돌아올 양수 흐름으로 취급)
     */
    public static OptionalDouble annualRatePct(List<ExternalFlow> flows, LocalDate valuationOn, long currentValue) {
        List<ExternalFlow> all = new ArrayList<>(flows);
        all.add(new ExternalFlow(valuationOn, currentValue));

        boolean hasPositive = all.stream().anyMatch(f -> f.amountKrw() > 0);
        boolean hasNegative = all.stream().anyMatch(f -> f.amountKrw() < 0);
        if (!hasPositive || !hasNegative) {
            return OptionalDouble.empty();
        }

        LocalDate base = all.stream().map(ExternalFlow::date).min(LocalDate::compareTo).orElse(valuationOn);
        double nLow = npv(all, base, LOW);
        double nHigh = npv(all, base, HIGH);
        if (nLow * nHigh > 0) {
            return OptionalDouble.empty();
        }

        double lo = LOW;
        double hi = HIGH;
        for (int i = 0; i < ITERATIONS; i++) {
            double mid = (lo + hi) / 2;
            double nMid = npv(all, base, mid);
            if (Math.abs(nMid) < 1e-4) {
                return OptionalDouble.of(mid * 100.0);
            }
            if (nLow * nMid < 0) {
                hi = mid;
            } else {
                lo = mid;
                nLow = nMid;
            }
        }
        return OptionalDouble.of((lo + hi) / 2 * 100.0);
    }

    private static double npv(List<ExternalFlow> flows, LocalDate base, double rate) {
        double sum = 0;
        for (ExternalFlow f : flows) {
            double years = ChronoUnit.DAYS.between(base, f.date()) / 365.0;
            sum += f.amountKrw() / Math.pow(1 + rate, years);
        }
        return sum;
    }
}
