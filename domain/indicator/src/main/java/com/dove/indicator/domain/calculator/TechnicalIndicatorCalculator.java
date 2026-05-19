package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public interface TechnicalIndicatorCalculator {

    String getName();

    int requiredDataSize();

    IndicatorType indicatorType();

    Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList);

    default Map<IndicatorType, Double> calculateWithSeed(List<DailyStockPrice> pool, double seed) {
        return calculate(pool);
    }

    /**
     * 누적·연속 계산이 필요한 지표 여부.
     * true 이면 커서 기반 순차 계산(EMA, OBV 등),
     * false 이면 날짜 단위 독립 계산(SMA, RSI 등).
     */
    default boolean isCumulative() {
        return false;
    }
}
