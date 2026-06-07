package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가격 위치비율 계산기. 20일·52주 고저 범위 및 20일 최저가 대비 현재 종가의 위치를 산출한다.
 */
@Component
public class PriceRangeRatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD_20D = 20;
    private static final int PERIOD_52W = 252;

    @Override
    public int requiredDataSize() {
        return PERIOD_52W;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.HIGH_20D_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD_52W) {
            return Map.of();
        }

        int size = dailyStockPriceList.size();
        long close = dailyStockPriceList.get(size - 1).getClosePrice();

        Map<IndicatorType, Double> result = new HashMap<>();
        result.put(IndicatorType.HIGH_20D_RATIO, computeRangeRatio(dailyStockPriceList, size - PERIOD_20D, size, close));
        result.put(IndicatorType.HIGH_52W_RATIO, computeRangeRatio(dailyStockPriceList, size - PERIOD_52W, size, close));

        // LOW_20D_RATIO = 현재 종가 / 20일 최저가 (지지선 대비 현재가 위치)
        long minLow = Long.MAX_VALUE;
        for (int i = size - PERIOD_20D; i < size; i++) {
            Long lp = dailyStockPriceList.get(i).getLowPrice();
            if (lp != null) minLow = Math.min(minLow, lp);
        }
        if (minLow > 0 && minLow != Long.MAX_VALUE) {
            result.put(IndicatorType.LOW_20D_RATIO, (double) close / minLow);
        }

        return result;
    }

    private double computeRangeRatio(List<StockPrice> list, int fromIndex, int toIndex, long close) {
        long maxHigh = Long.MIN_VALUE;
        long minLow = Long.MAX_VALUE;

        for (int i = fromIndex; i < toIndex; i++) {
            StockPrice price = list.get(i);
            maxHigh = Math.max(maxHigh, price.getHighPrice());
            minLow = Math.min(minLow, price.getLowPrice());
        }

        if (maxHigh == minLow) {
            return 0.0;
        }

        return (double) (close - minLow) / (maxHigh - minLow);
    }
}
