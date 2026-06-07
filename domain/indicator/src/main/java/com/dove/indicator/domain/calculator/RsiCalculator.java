package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 상대강도지수(RSI) 계산기. 지정 기간의 평균 상승폭/하락폭 비율로 과매수·과매도 수준을 산출한다.
 */
public class RsiCalculator implements TechnicalIndicatorCalculator {

    private final int period;
    private final IndicatorType indicatorType;

    public RsiCalculator(int period, IndicatorType indicatorType) {
        this.period = period;
        this.indicatorType = indicatorType;
    }

    @Override
    public int requiredDataSize() {
        return period * 5;
    }

    @Override
    public IndicatorType indicatorType() {
        return indicatorType;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        double gainSum = 0;
        double lossSum = 0;

        for (int i = 1; i <= period; i++) {
            double change = dailyStockPriceList.get(i).getClosePrice() - dailyStockPriceList.get(i - 1).getClosePrice();
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        for (int i = period + 1; i < dailyStockPriceList.size(); i++) {
            double change = dailyStockPriceList.get(i).getClosePrice() - dailyStockPriceList.get(i - 1).getClosePrice();
            double currentGain = Math.max(change, 0);
            double currentLoss = Math.max(-change, 0);
            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;
        }

        if (avgLoss == 0) {
            return Map.of(indicatorType, 100.0);
        }

        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));

        return Map.of(indicatorType, rsi);
    }
}
