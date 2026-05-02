package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 단순이동평균(SMA) 계산기. 지정 기간의 종가 평균을 계산한다.
 */
public class SmaCalculator implements TechnicalIndicatorCalculator {

    private final int period;
    private final IndicatorType indicatorType;

    public SmaCalculator(int period, IndicatorType indicatorType) {
        this.period = period;
        this.indicatorType = indicatorType;
    }

    @Override
    public String getName() {
        return indicatorType.name();
    }

    @Override
    public int requiredDataSize() {
        return period;
    }

    @Override
    public IndicatorType cursorType() {
        return indicatorType;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        double sum = dailyStockPriceList.stream()
                .mapToLong(DailyStockPrice::getClosePrice)
                .sum();

        double sma = sum / period;
        return Map.of(indicatorType, sma);
    }
}
