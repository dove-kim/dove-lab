package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 일별 주가 시계열로부터 하나 이상의 기술적 지표 값을 산출하는 계산기.
 */
public interface TechnicalIndicatorCalculator {

    /**
     * 한 값을 산출하는 데 필요한 최소 일별 주가 개수.
     */
    int requiredDataSize();

    /**
     * 이 계산기의 대표 지표 종류.
     */
    IndicatorType indicatorType();

    /**
     * 주어진 주가 풀로부터 지표 값을 계산한다.
     */
    Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList);

    /**
     * 직전 시드 값을 이어받아 누적 지표를 계산한다. 기본 구현은 {@link #calculate}에 위임한다.
     */
    default Map<IndicatorType, Double> calculateWithSeed(List<StockPrice> pool, double seed) {
        return calculate(pool);
    }

    /**
     * 누적·연속 계산(시드 유지)이 필요한 지표 여부.
     */
    default boolean isCumulative() {
        return false;
    }

    /**
     * 유한 lookback으로 시드를 복원할 수 없어(비감쇠 누적) 직전 거래일의 저장값을 시드로 이어받아야 하는 지표 여부.
     */
    default boolean requiresPersistedSeed() {
        return false;
    }
}
