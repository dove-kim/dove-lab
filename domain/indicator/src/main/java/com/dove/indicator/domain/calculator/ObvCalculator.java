package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public class ObvCalculator implements TechnicalIndicatorCalculator {

    @Override
    public String getName() {
        return "OBV";
    }

    @Override
    public int requiredDataSize() {
        return 2;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.OBV;
    }

    @Override
    public boolean isCumulative() {
        return true;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        return calculateWithSeed(dailyStockPriceList, 0.0);
    }

    @Override
    public Map<IndicatorType, Double> calculateWithSeed(List<DailyStockPrice> pool, double seed) {
        double obv = seed;

        for (int i = 1; i < pool.size(); i++) {
            long currentClose = pool.get(i).getClosePrice();
            long previousClose = pool.get(i - 1).getClosePrice();
            long volume = pool.get(i).getVolume();

            if (currentClose > previousClose) {
                obv += volume;
            } else if (currentClose < previousClose) {
                obv -= volume;
            }
        }

        return Map.of(IndicatorType.OBV, obv);
    }
}
