package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public interface TechnicalIndicatorCalculator {

    String getName();

    int requiredDataSize();

    IndicatorType cursorType();

    Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList);

    default Map<IndicatorType, Double> calculateWithSeed(List<DailyStockPrice> pool, double seed) {
        return calculate(pool);
    }
}
