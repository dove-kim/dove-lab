package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * CCI(Commodity Channel Index) 계산기. 20일 Typical Price의 평균 편차를 기준으로 추세를 판단한다.
 */
public class CciCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 20;
    private static final double CONSTANT = 0.015;

    @Override
    public String getName() {
        return "CCI";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.CCI;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        double[] typicalPrices = new double[dailyStockPriceList.size()];
        for (int i = 0; i < dailyStockPriceList.size(); i++) {
            DailyStockPrice data = dailyStockPriceList.get(i);
            typicalPrices[i] = (data.getHighPrice() + data.getLowPrice() + data.getClosePrice()) / 3.0;
        }

        double tpSum = 0;
        for (double tp : typicalPrices) {
            tpSum += tp;
        }
        double smaTp = tpSum / PERIOD;

        double meanDeviationSum = 0;
        for (double tp : typicalPrices) {
            meanDeviationSum += Math.abs(tp - smaTp);
        }
        double meanDeviation = meanDeviationSum / PERIOD;

        if (meanDeviation == 0) {
            return Map.of(IndicatorType.CCI, 0.0);
        }

        double lastTp = typicalPrices[typicalPrices.length - 1];
        double cci = (lastTp - smaTp) / (CONSTANT * meanDeviation);

        return Map.of(IndicatorType.CCI, cci);
    }
}
