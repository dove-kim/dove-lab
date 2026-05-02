package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 스토캐스틱 계산기. %K(14일)와 %D(7일 평균)로 과매수/과매도 수준을 판단한다.
 */
public class StochasticCalculator implements TechnicalIndicatorCalculator {

    private static final int K_PERIOD = 14;
    private static final int D_PERIOD = 7;
    private static final int REQUIRED_SIZE = K_PERIOD + D_PERIOD - 1;

    @Override
    public String getName() {
        return "STOCHASTIC";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.STOCHASTIC_K;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        int totalSize = dailyStockPriceList.size();
        int kCount = totalSize - K_PERIOD + 1;
        double[] kValues = new double[kCount];

        for (int i = 0; i < kCount; i++) {
            long highestHigh = Long.MIN_VALUE;
            long lowestLow = Long.MAX_VALUE;

            for (int j = i; j < i + K_PERIOD; j++) {
                DailyStockPrice data = dailyStockPriceList.get(j);
                highestHigh = Math.max(highestHigh, data.getHighPrice());
                lowestLow = Math.min(lowestLow, data.getLowPrice());
            }

            long close = dailyStockPriceList.get(i + K_PERIOD - 1).getClosePrice();

            if (highestHigh == lowestLow) {
                kValues[i] = 100.0;
            } else {
                kValues[i] = ((double) (close - lowestLow) / (highestHigh - lowestLow)) * 100.0;
            }
        }

        double percentK = kValues[kValues.length - 1];

        double dSum = 0;
        for (int i = kValues.length - D_PERIOD; i < kValues.length; i++) {
            dSum += kValues[i];
        }
        double percentD = dSum / D_PERIOD;

        return Map.of(
                IndicatorType.STOCHASTIC_K, percentK,
                IndicatorType.STOCHASTIC_D, percentD
        );
    }
}
