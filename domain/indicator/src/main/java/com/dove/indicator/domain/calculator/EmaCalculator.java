package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 지수이동평균(EMA) 계산기. 최근 가격에 가중치를 둔 지정 기간 종가 평균을 계산한다.
 */
public class EmaCalculator implements TechnicalIndicatorCalculator {

    private final int period;
    private final IndicatorType indicatorType;

    public EmaCalculator(int period, IndicatorType indicatorType) {
        this.period = period;
        this.indicatorType = indicatorType;
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
    public Map<IndicatorType, Double> calculate(List<StockPrice> pool) {
        double multiplier = 2.0 / (period + 1);
        double ema = pool.stream().limit(period).mapToLong(StockPrice::getClosePrice).average().orElse(0);
        for (int i = period; i < pool.size(); i++) {
            ema = pool.get(i).getClosePrice() * multiplier + ema * (1 - multiplier);
        }
        return Map.of(indicatorType, ema);
    }

    @Override
    public boolean isCumulative() {
        return true;
    }

    /**
     * EMA는 무한기억 재귀식이라 유한 lookback을 SMA로 재시드하면 불연속이 생긴다.
     * 재개·rewind 시 직전 거래일의 저장 EMA를 시드로 이어받아 연속성을 유지한다.
     */
    @Override
    public boolean requiresPersistedSeed() {
        return true;
    }

    @Override
    public Map<IndicatorType, Double> calculateWithSeed(List<StockPrice> pool, double seed) {
        double multiplier = 2.0 / (period + 1);
        double closePrice = pool.get(pool.size() - 1).getClosePrice();
        double ema = closePrice * multiplier + seed * (1 - multiplier);
        return Map.of(indicatorType, ema);
    }
}
