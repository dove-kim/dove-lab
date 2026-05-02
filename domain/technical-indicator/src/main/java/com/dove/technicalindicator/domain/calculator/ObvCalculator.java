package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

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
    public IndicatorType cursorType() {
        return IndicatorType.OBV;
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
