package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 캔들 형태 지표 계산기. 몸통 비율(BODY_RATIO)과 아랫꼬리 비율(LOWER_WICK)을 산출한다.
 */
@Component
public class CandlePatternCalculator implements TechnicalIndicatorCalculator {

    @Override
    public int requiredDataSize() {
        return 1;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.BODY_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> prices) {
        if (prices.isEmpty()) return Map.of();

        StockPrice p = prices.get(prices.size() - 1);
        long close  = p.getClosePrice();
        Long open   = p.getOpenPrice();
        Long low    = p.getLowPrice();

        if (close <= 0) return Map.of();

        Map<IndicatorType, Double> result = new EnumMap<>(IndicatorType.class);

        if (open != null) {
            result.put(IndicatorType.BODY_RATIO, (double) (close - open) / close);

            if (low != null) {
                long bodyLow = Math.min(open, close);
                result.put(IndicatorType.LOWER_WICK, (double) (bodyLow - low) / close);
            }
        }

        return result;
    }
}
