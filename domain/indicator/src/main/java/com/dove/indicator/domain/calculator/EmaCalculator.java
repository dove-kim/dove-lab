package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public class EmaCalculator implements TechnicalIndicatorCalculator {

    private final int period;
    private final IndicatorType indicatorType;

    public EmaCalculator(int period, IndicatorType indicatorType) {
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
    public IndicatorType indicatorType() {
        return indicatorType;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> pool) {
        double multiplier = 2.0 / (period + 1);
        double ema = pool.stream().limit(period).mapToLong(DailyStockPrice::getClosePrice).average().orElse(0);
        for (int i = period; i < pool.size(); i++) {
            ema = pool.get(i).getClosePrice() * multiplier + ema * (1 - multiplier);
        }
        return Map.of(indicatorType, ema);
    }

    @Override
    public boolean isCumulative() {
        return true;
    }

    @Override
    public Map<IndicatorType, Double> calculateWithSeed(List<DailyStockPrice> pool, double seed) {
        double multiplier = 2.0 / (period + 1);
        double closePrice = pool.get(pool.size() - 1).getClosePrice();
        double ema = closePrice * multiplier + seed * (1 - multiplier);
        return Map.of(indicatorType, ema);
    }
}
