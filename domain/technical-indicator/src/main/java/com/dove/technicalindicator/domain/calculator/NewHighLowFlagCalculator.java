package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewHighLowFlagCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD_52W = 252;
    private static final int PERIOD_20D = 20;

    @Override
    public String getName() {
        return "NEW_HIGH_LOW_FLAG";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD_52W;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.IS_52W_HIGH;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD_52W) {
            return Collections.emptyMap();
        }

        long lastClose = dailyStockPriceList.get(dailyStockPriceList.size() - 1).getClosePrice();

        long max52w = dailyStockPriceList.stream().mapToLong(DailyStockPrice::getClosePrice).max().orElse(Long.MIN_VALUE);
        long min52w = dailyStockPriceList.stream().mapToLong(DailyStockPrice::getClosePrice).min().orElse(Long.MAX_VALUE);

        List<DailyStockPrice> last20 = dailyStockPriceList.subList(dailyStockPriceList.size() - PERIOD_20D, dailyStockPriceList.size());
        long max20d = last20.stream().mapToLong(DailyStockPrice::getClosePrice).max().orElse(Long.MIN_VALUE);
        long min20d = last20.stream().mapToLong(DailyStockPrice::getClosePrice).min().orElse(Long.MAX_VALUE);

        Map<IndicatorType, Double> result = new HashMap<>();
        result.put(IndicatorType.IS_52W_HIGH, lastClose == max52w ? 1.0 : 0.0);
        result.put(IndicatorType.IS_52W_LOW, lastClose == min52w ? 1.0 : 0.0);
        result.put(IndicatorType.IS_20D_HIGH, lastClose == max20d ? 1.0 : 0.0);
        result.put(IndicatorType.IS_20D_LOW, lastClose == min20d ? 1.0 : 0.0);
        return result;
    }
}
