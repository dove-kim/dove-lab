package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

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
    public IndicatorType cursorType() {
        return IndicatorType.BB_UPPER;
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

        double close = dailyStockPriceList.get(dailyStockPriceList.size() - 1).getClosePrice();
        double bandWidth = upper - lower;
        double percentB = bandWidth == 0.0 ? 0.0 : (close - lower) / bandWidth;
        double bbWidth = bandWidth / middle;

        return Map.of(
                IndicatorType.BB_UPPER, upper,
                IndicatorType.BB_MIDDLE, middle,
                IndicatorType.BB_LOWER, lower,
                IndicatorType.BB_PERCENT_B, percentB,
                IndicatorType.BB_WIDTH, bbWidth
        );
    }
}
