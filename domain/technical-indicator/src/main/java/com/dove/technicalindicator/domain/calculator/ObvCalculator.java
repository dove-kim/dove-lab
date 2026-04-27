package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * OBV(On Balance Volume) 계산기. 종가 상승 시 거래량을 누적하여 매집/분산을 추적한다.
 */
public class ObvCalculator implements TechnicalIndicatorCalculator {

    @Override
    public String getName() {
        return "OBV";
    }

    @Override
    public int requiredDataSize() {
        return 2;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        long obv = 0;

        for (int i = 1; i < dailyStockPriceList.size(); i++) {
            long currentClose = dailyStockPriceList.get(i).getClosePrice();
            long previousClose = dailyStockPriceList.get(i - 1).getClosePrice();
            long volume = dailyStockPriceList.get(i).getVolume();

            if (currentClose > previousClose) {
                obv += volume;
            } else if (currentClose < previousClose) {
                obv -= volume;
            }
        }

        return Map.of(IndicatorType.OBV, (double) obv);
    }
}
