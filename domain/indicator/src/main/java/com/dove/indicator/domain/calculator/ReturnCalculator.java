package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 과거 수익률 지표 계산기. 1·5·10거래일 전 종가 대비 수익률(RET_1D/5D/10D)을 산출한다.
 */
@Component
public class ReturnCalculator implements TechnicalIndicatorCalculator {

    private static final int MAX_PERIOD = 10;

    @Override
    public int requiredDataSize() {
        return MAX_PERIOD + 1; // 11
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.RET_1D;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> prices) {
        int size = prices.size();
        if (size < 2) return Map.of();

        long close = prices.get(size - 1).getClosePrice();
        Map<IndicatorType, Double> result = new EnumMap<>(IndicatorType.class);

        // RET_1D
        long prev1 = prices.get(size - 2).getClosePrice();
        if (prev1 > 0) result.put(IndicatorType.RET_1D, (double) (close - prev1) / prev1);

        // RET_5D
        if (size >= 6) {
            long prev5 = prices.get(size - 6).getClosePrice();
            if (prev5 > 0) result.put(IndicatorType.RET_5D, (double) (close - prev5) / prev5);
        }

        // RET_10D
        if (size >= 11) {
            long prev10 = prices.get(size - 11).getClosePrice();
            if (prev10 > 0) result.put(IndicatorType.RET_10D, (double) (close - prev10) / prev10);
        }

        return result;
    }
}
