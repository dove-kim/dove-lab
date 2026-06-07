package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 거래량 이평 대비 비율 계산기. 당일 거래량을 20일 평균 거래량으로 나눈 비율을 산출한다.
 */
@Component
public class VolumeMa20RatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 20;

    @Override
    public int requiredDataSize() {
        return PERIOD;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.VOLUME_MA20_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD) {
            return Map.of();
        }

        int size = dailyStockPriceList.size();
        List<StockPrice> window = dailyStockPriceList.subList(size - PERIOD, size);

        double average = window.stream()
                .mapToLong(StockPrice::getVolume)
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
