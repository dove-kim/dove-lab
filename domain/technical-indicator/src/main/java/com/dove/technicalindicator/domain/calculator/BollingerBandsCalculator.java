package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 볼린저 밴드 계산기. 20일 SMA를 중심으로 표준편차 x2 상/하단 밴드를 산출한다.
 */
public class BollingerBandsCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 20;
    private static final double MULTIPLIER = 2.0;

    @Override
    public String getName() {
        return "BOLLINGER_BANDS";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        double sum = 0;
        for (DailyStockPrice data : dailyStockPriceList) {
            sum += data.getClosePrice();
        }
        double middle = sum / PERIOD;

        double varianceSum = 0;
        for (DailyStockPrice data : dailyStockPriceList) {
            double diff = data.getClosePrice() - middle;
            varianceSum += diff * diff;
        }
        double stdDev = Math.sqrt(varianceSum / PERIOD);

        double upper = middle + MULTIPLIER * stdDev;
        double lower = middle - MULTIPLIER * stdDev;

        return Map.of(
                IndicatorType.BB_UPPER, upper,
                IndicatorType.BB_MIDDLE, middle,
                IndicatorType.BB_LOWER, lower
        );
    }
}
