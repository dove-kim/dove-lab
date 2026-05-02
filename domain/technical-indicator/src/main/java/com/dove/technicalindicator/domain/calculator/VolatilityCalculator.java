package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VolatilityCalculator implements TechnicalIndicatorCalculator {

    private static final int REQUIRED_SIZE = 21;
    private static final int LONG_PERIOD = 20;
    private static final int SHORT_PERIOD = 5;

    @Override
    public String getName() {
        return "VOLATILITY";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.VOLATILITY_5D;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < REQUIRED_SIZE) {
            return Collections.emptyMap();
        }

        int n = dailyStockPriceList.size();
        double[] logReturns = new double[LONG_PERIOD];
        for (int i = 0; i < LONG_PERIOD; i++) {
            int idx = n - LONG_PERIOD + i;
            double current = dailyStockPriceList.get(idx).getClosePrice();
            double previous = dailyStockPriceList.get(idx - 1).getClosePrice();
            logReturns[i] = Math.log(current / previous);
        }

        double vol5d = sampleStdDev(logReturns, LONG_PERIOD - SHORT_PERIOD, LONG_PERIOD);
        double vol20d = sampleStdDev(logReturns, 0, LONG_PERIOD);

        return Map.of(
                IndicatorType.VOLATILITY_5D, vol5d,
                IndicatorType.VOLATILITY_20D, vol20d
        );
    }

    private double sampleStdDev(double[] data, int fromIndex, int toIndex) {
        int count = toIndex - fromIndex;
        double mean = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            mean += data[i];
        }
        mean /= count;

        double sumSq = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            double diff = data[i] - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (count - 1));
    }
}
