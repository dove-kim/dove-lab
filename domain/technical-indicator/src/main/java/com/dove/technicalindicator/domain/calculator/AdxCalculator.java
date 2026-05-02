package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * ADX(평균 방향성 지수) 계산기. 14일 기간의 ADX, +DI, -DI를 산출하여 추세 강도를 측정한다.
 */
public class AdxCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 14;
    private static final int REQUIRED_SIZE = 100;

    @Override
    public String getName() {
        return "ADX";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.ADX;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        int size = dailyStockPriceList.size();

        double[] tr = new double[size - 1];
        double[] plusDm = new double[size - 1];
        double[] minusDm = new double[size - 1];

        for (int i = 1; i < size; i++) {
            DailyStockPrice current = dailyStockPriceList.get(i);
            DailyStockPrice previous = dailyStockPriceList.get(i - 1);

            double highDiff = current.getHighPrice() - previous.getHighPrice();
            double lowDiff = previous.getLowPrice() - current.getLowPrice();

            plusDm[i - 1] = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
            minusDm[i - 1] = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;

            double hl = current.getHighPrice() - current.getLowPrice();
            double hc = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lc = Math.abs(current.getLowPrice() - previous.getClosePrice());
            tr[i - 1] = Math.max(hl, Math.max(hc, lc));
        }

        double smoothedTr = 0;
        double smoothedPlusDm = 0;
        double smoothedMinusDm = 0;

        for (int i = 0; i < PERIOD; i++) {
            smoothedTr += tr[i];
            smoothedPlusDm += plusDm[i];
            smoothedMinusDm += minusDm[i];
        }

        double[] dx = new double[tr.length - PERIOD + 1];

        double plusDi = (smoothedTr == 0) ? 0 : (smoothedPlusDm / smoothedTr) * 100;
        double minusDi = (smoothedTr == 0) ? 0 : (smoothedMinusDm / smoothedTr) * 100;
        double diSum = plusDi + minusDi;
        dx[0] = (diSum == 0) ? 0 : Math.abs(plusDi - minusDi) / diSum * 100;

        for (int i = PERIOD; i < tr.length; i++) {
            smoothedTr = smoothedTr - (smoothedTr / PERIOD) + tr[i];
            smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / PERIOD) + plusDm[i];
            smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / PERIOD) + minusDm[i];

            plusDi = (smoothedTr == 0) ? 0 : (smoothedPlusDm / smoothedTr) * 100;
            minusDi = (smoothedTr == 0) ? 0 : (smoothedMinusDm / smoothedTr) * 100;
            diSum = plusDi + minusDi;
            dx[i - PERIOD + 1] = (diSum == 0) ? 0 : Math.abs(plusDi - minusDi) / diSum * 100;
        }

        double adxSum = 0;
        for (double d : dx) {
            adxSum += d;
        }
        double adx = adxSum / dx.length;

        return Map.of(
                IndicatorType.ADX, adx,
                IndicatorType.PLUS_DI, plusDi,
                IndicatorType.MINUS_DI, minusDi
        );
    }
}
