package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 20일 고점 돌파 계산기. 당일 종가가 직전 20일 최고가를 상향 돌파했는지 여부(1/0)를 산출한다.
 */
@Component
public class BreakoutCalculator implements TechnicalIndicatorCalculator {

    private static final int LOOKBACK = 20;
    private static final int REQUIRED_SIZE = LOOKBACK + 1; // 21

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.BREAKOUT_20D;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> prices) {
        int size = prices.size();
        if (size < REQUIRED_SIZE) return Map.of();

        Long close = prices.get(size - 1).getClosePrice();
        if (close == null) return Map.of();

        long prev20High = Long.MIN_VALUE;
        for (int i = size - 1 - LOOKBACK; i < size - 1; i++) {
            Long high = prices.get(i).getHighPrice();
            if (high == null) return Map.of();
            if (high > prev20High) prev20High = high;
        }

        return Map.of(IndicatorType.BREAKOUT_20D, close > prev20High ? 1.0 : 0.0);
    }
}
