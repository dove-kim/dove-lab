package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public class VolumeMa20RatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 20;

    @Override
    public String getName() {
        return "VOLUME_MA20_RATIO";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.VOLUME_MA20_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD) {
            return Map.of();
        }

        int size = dailyStockPriceList.size();
        List<DailyStockPrice> window = dailyStockPriceList.subList(size - PERIOD, size);

        double average = window.stream()
                .mapToLong(DailyStockPrice::getVolume)
                .average()
                .orElse(0.0);

        if (average == 0.0) {
            return Map.of(IndicatorType.VOLUME_MA20_RATIO, 0.0);
        }

        long lastVolume = window.get(PERIOD - 1).getVolume();
        double ratio = lastVolume / average;

        return Map.of(IndicatorType.VOLUME_MA20_RATIO, ratio);
    }
}
