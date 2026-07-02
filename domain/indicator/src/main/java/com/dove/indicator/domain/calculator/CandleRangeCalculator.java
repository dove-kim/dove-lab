package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 당일 범위 기준 캔들 비율 계산기. 윗꼬리 비율(UPPER_WICK_RATIO)과 종가 위치(CLOSE_POS)를 산출한다.
 */
@Component
public class CandleRangeCalculator implements TechnicalIndicatorCalculator {

    @Override
    public int requiredDataSize() {
        return 1;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.UPPER_WICK_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> prices) {
        if (prices.isEmpty()) return Map.of();

        StockPrice p = prices.get(prices.size() - 1);
        Long open = p.getOpenPrice();
        Long high = p.getHighPrice();
        Long low = p.getLowPrice();
        Long close = p.getClosePrice();
        if (open == null || high == null || low == null || close == null) return Map.of();

        long range = high - low;
        if (range == 0) return Map.of(); // 분모 0 — features.py와 동일하게 NaN(미산출)

        Map<IndicatorType, Double> result = new EnumMap<>(IndicatorType.class);
        result.put(IndicatorType.UPPER_WICK_RATIO, (double) (high - Math.max(open, close)) / range);
        result.put(IndicatorType.CLOSE_POS, (double) (close - low) / range);
        return result;
    }
}
