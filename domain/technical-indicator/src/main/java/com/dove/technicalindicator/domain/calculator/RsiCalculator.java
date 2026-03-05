package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 상대강도지수(RSI) 계산기. 14일 기간의 평균 상승/하락폭 비율로 과매수/과매도를 판단한다.
 */
public class RsiCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 14;

    @Override
    public String getName() {
        return "RSI_14";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD + 1;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockData> stockDataList) {
        double gainSum = 0;
        double lossSum = 0;

        for (int i = 1; i <= PERIOD; i++) {
            double change = stockDataList.get(i).getClosePrice() - stockDataList.get(i - 1).getClosePrice();
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double avgGain = gainSum / PERIOD;
        double avgLoss = lossSum / PERIOD;

        if (avgLoss == 0) {
            return Map.of(IndicatorType.RSI_14, 100.0);
        }

        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));

        return Map.of(IndicatorType.RSI_14, rsi);
    }
}
