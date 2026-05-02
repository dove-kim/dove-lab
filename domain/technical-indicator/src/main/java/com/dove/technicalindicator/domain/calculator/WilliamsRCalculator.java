package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * Williams %R 계산기. 14일간 고가-저가 범위 대비 현재 종가 위치를 -100~0 범위로 산출한다.
 */
public class WilliamsRCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 14;

    @Override
    public String getName() {
        return "WILLIAMS_R";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.WILLIAMS_R;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        long highestHigh = Long.MIN_VALUE;
        long lowestLow = Long.MAX_VALUE;

        for (DailyStockPrice data : dailyStockPriceList) {
            highestHigh = Math.max(highestHigh, data.getHighPrice());
            lowestLow = Math.min(lowestLow, data.getLowPrice());
        }

        long close = dailyStockPriceList.get(dailyStockPriceList.size() - 1).getClosePrice();

        if (highestHigh == lowestLow) {
            return Map.of(IndicatorType.WILLIAMS_R, 0.0);
        }

        double williamsR = ((double) (highestHigh - close) / (highestHigh - lowestLow)) * -100.0;

        return Map.of(IndicatorType.WILLIAMS_R, williamsR);
    }
}
