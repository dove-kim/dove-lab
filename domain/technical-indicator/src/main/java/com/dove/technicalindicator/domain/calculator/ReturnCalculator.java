package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnCalculator implements TechnicalIndicatorCalculator {

    private static final int REQUIRED_SIZE = 21;

    @Override
    public String getName() {
        return "RETURN";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.RETURN_1D;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < REQUIRED_SIZE) {
            return Collections.emptyMap();
        }

        int last = dailyStockPriceList.size() - 1;
        double closeT = dailyStockPriceList.get(last).getClosePrice();

        Map<IndicatorType, Double> result = new HashMap<>();
        result.put(IndicatorType.RETURN_1D, logReturn(closeT, dailyStockPriceList.get(last - 1).getClosePrice()));
        result.put(IndicatorType.RETURN_3D, logReturn(closeT, dailyStockPriceList.get(last - 3).getClosePrice()));
        result.put(IndicatorType.RETURN_5D, logReturn(closeT, dailyStockPriceList.get(last - 5).getClosePrice()));
        result.put(IndicatorType.RETURN_10D, logReturn(closeT, dailyStockPriceList.get(last - 10).getClosePrice()));
        result.put(IndicatorType.RETURN_20D, logReturn(closeT, dailyStockPriceList.get(last - 20).getClosePrice()));
        return result;
    }

    private double logReturn(double current, double previous) {
        return Math.log(current / previous);
    }
}
