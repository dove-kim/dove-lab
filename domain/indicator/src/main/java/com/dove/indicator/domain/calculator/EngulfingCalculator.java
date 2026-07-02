package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 장악형(엔걸핑) 캔들 패턴 계산기. 당일·전일 봉의 관계로 상승/하락 장악형 여부(1/0)를 산출한다.
 */
@Component
public class EngulfingCalculator implements TechnicalIndicatorCalculator {

    private static final int REQUIRED_SIZE = 2;

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.BULLISH_ENGULFING;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> prices) {
        int size = prices.size();
        if (size < REQUIRED_SIZE) return Map.of();

        StockPrice today = prices.get(size - 1);
        StockPrice prev = prices.get(size - 2);
        Long o = today.getOpenPrice();
        Long c = today.getClosePrice();
        Long po = prev.getOpenPrice();
        Long pc = prev.getClosePrice();
        if (o == null || c == null || po == null || pc == null) return Map.of();

        boolean bull = c > o;
        boolean bear = c < o;
        boolean prevBear = pc < po;
        boolean prevBull = pc > po;

        boolean bullish = bull && prevBear && o <= pc && c >= po;
        boolean bearish = bear && prevBull && o >= pc && c <= po;

        Map<IndicatorType, Double> result = new EnumMap<>(IndicatorType.class);
        result.put(IndicatorType.BULLISH_ENGULFING, bullish ? 1.0 : 0.0);
        result.put(IndicatorType.BEARISH_ENGULFING, bearish ? 1.0 : 0.0);
        return result;
    }
}
