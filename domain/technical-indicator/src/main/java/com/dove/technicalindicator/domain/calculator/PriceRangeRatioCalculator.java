package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceRangeRatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD_20D = 20;
    private static final int PERIOD_52W = 252;

    @Override
    public String getName() {
        return "PRICE_RANGE_RATIO";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD_52W;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.HIGH_20D_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD_52W) {
            return Map.of();
        }

        int size = dailyStockPriceList.size();
        long close = dailyStockPriceList.get(size - 1).getClosePrice();

        Map<IndicatorType, Double> result = new HashMap<>();
        result.put(IndicatorType.HIGH_20D_RATIO, computeRangeRatio(dailyStockPriceList, size - PERIOD_20D, size, close));
        result.put(IndicatorType.HIGH_52W_RATIO, computeRangeRatio(dailyStockPriceList, size - PERIOD_52W, size, close));
        return result;
    }

    private double computeRangeRatio(List<DailyStockPrice> list, int fromIndex, int toIndex, long close) {
        long maxHigh = Long.MIN_VALUE;
        long minLow = Long.MAX_VALUE;

        for (int i = fromIndex; i < toIndex; i++) {
            DailyStockPrice price = list.get(i);
            maxHigh = Math.max(maxHigh, price.getHighPrice());
            minLow = Math.min(minLow, price.getLowPrice());
        }

        if (maxHigh == minLow) {
            return 0.0;
        }

        return (double) (close - minLow) / (maxHigh - minLow);
    }
}
