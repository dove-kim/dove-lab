package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;

import java.util.List;
import java.util.Map;

/**
 * 한 계산 패스(한 그룹·한 청크) 동안 누적 지표의 시드 연속성을 유지하며 계산기를 구동한다.
 * 비누적 계산기는 윈도우마다 새로 계산하고, 누적 계산기는 첫 완전 윈도우에서 초기화한 뒤 직전 시드를 이어받는다.
 */
public final class CalculatorRunner {

    private final TechnicalIndicatorCalculator calculator;
    private double seed;
    private boolean firstFullPool = true;

    public CalculatorRunner(TechnicalIndicatorCalculator calculator) {
        this(calculator, null);
    }

    /**
     * 누적 시드 초기값을 받아 러너를 만든다. 직전 거래일의 저장값에서 이어 계산하는 비감쇠 누적 지표에 쓴다.
     */
    public CalculatorRunner(TechnicalIndicatorCalculator calculator, Double initialSeed) {
        this.calculator = calculator;
        if (initialSeed != null) {
            this.seed = initialSeed;
            this.firstFullPool = false;
        }
    }

    /**
     * 계산기가 한 값을 산출하는 데 필요한 최소 데이터 개수.
     */
    public int requiredDataSize() {
        return calculator.requiredDataSize();
    }

    /**
     * 직전 거래일의 저장 시드를 이어받아야 하는(유한 lookback 재시드 불가) 지표 여부.
     */
    public boolean requiresPersistedSeed() {
        return calculator.requiresPersistedSeed();
    }

    /**
     * 주어진 윈도우로 지표 값을 계산한다. 누적 계산기는 직전 시드를 이어받아 연속 계산한다.
     */
    public Map<IndicatorType, Double> compute(List<StockPrice> pool) {
        if (!calculator.isCumulative()) {
            return calculator.calculate(pool);
        }
        Map<IndicatorType, Double> result = firstFullPool
                ? calculator.calculate(pool)
                : calculator.calculateWithSeed(pool, seed);
        firstFullPool = false;
        seed = result.getOrDefault(calculator.indicatorType(), seed);
        return result;
    }
}
