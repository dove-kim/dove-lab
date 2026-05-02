package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

public class MaRatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD_5 = 5;
    private static final int PERIOD_20 = 20;
    private static final int PERIOD_60 = 60;

    @Override
    public String getName() {
        return "MA_RATIO";
    }

    @Override
    public int requiredDataSize() {
        return PERIOD_60;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.MA5_MA20_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < PERIOD_60) {
            return Map.of();
        }

        double sma5 = sma(dailyStockPriceList, PERIOD_5);
        double sma20 = sma(dailyStockPriceList, PERIOD_20);
        double sma60 = sma(dailyStockPriceList, PERIOD_60);

        return Map.of(
                IndicatorType.MA5_MA20_RATIO, sma5 / sma20,
                IndicatorType.MA20_MA60_RATIO, sma20 / sma60
        );
    }

    private double sma(List<DailyStockPrice> data, int period) {
        int size = data.size();
        return data.subList(size - period, size).stream()
                .mapToLong(DailyStockPrice::getClosePrice)
                .average()
                .orElse(0.0);
    }
}
