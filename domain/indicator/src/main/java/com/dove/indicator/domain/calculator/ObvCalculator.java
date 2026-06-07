package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OBV(On-Balance Volume) 계산기. 종가 등락에 따라 거래량을 누적해 매집·분산 강도를 측정한다.
 */
@Component
public class ObvCalculator implements TechnicalIndicatorCalculator {

    @Override
    public int requiredDataSize() {
        return 2;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.OBV;
    }

    @Override
    public boolean isCumulative() {
        return true;
    }

    @Override
    public boolean requiresPersistedSeed() {
        return true;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        return calculateWithSeed(dailyStockPriceList, 0.0);
    }

    @Override
    public Map<IndicatorType, Double> calculateWithSeed(List<StockPrice> pool, double seed) {
        double obv = seed;

        for (int i = 1; i < pool.size(); i++) {
            long currentClose = pool.get(i).getClosePrice();
            long previousClose = pool.get(i - 1).getClosePrice();
            long volume = pool.get(i).getVolume();

            if (currentClose > previousClose) {
                obv += volume;
            } else if (currentClose < previousClose) {
                obv -= volume;
            }
        }

        return Map.of(IndicatorType.OBV, obv);
    }
}
