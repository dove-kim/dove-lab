package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * ATR(Average True Range) 계산기. 14일간 True Range 평균으로 변동성을 측정한다.
 */
public class AtrCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 14;

    @Override
    public String getName() {
        return "ATR";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD + 1;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockData> stockDataList) {
        double[] trueRanges = new double[stockDataList.size() - 1];

        for (int i = 1; i < stockDataList.size(); i++) {
            StockData current = stockDataList.get(i);
            StockData previous = stockDataList.get(i - 1);

            double hl = current.getHighPrice() - current.getLowPrice();
            double hc = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lc = Math.abs(current.getLowPrice() - previous.getClosePrice());

            trueRanges[i - 1] = Math.max(hl, Math.max(hc, lc));
        }

        double atr = 0;
        for (int i = 0; i < PERIOD; i++) {
            atr += trueRanges[i];
        }
        atr /= PERIOD;

        for (int i = PERIOD; i < trueRanges.length; i++) {
            atr = (atr * (PERIOD - 1) + trueRanges[i]) / PERIOD;
        }

        return Map.of(IndicatorType.ATR, atr);
    }
}
