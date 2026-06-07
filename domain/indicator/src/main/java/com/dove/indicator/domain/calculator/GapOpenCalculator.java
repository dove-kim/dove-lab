package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 갭 상승 계산기. 당일 시가를 전일 종가와 비교한 갭 비율을 산출한다.
 */
@Component
public class GapOpenCalculator implements TechnicalIndicatorCalculator {

    private static final int REQUIRED_SIZE = 2;

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.GAP_OPEN;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        if (dailyStockPriceList.size() < REQUIRED_SIZE) {
            return Map.of();
        }

        int size = dailyStockPriceList.size();
        long todayOpen = dailyStockPriceList.get(size - 1).getOpenPrice();
        long prevClose = dailyStockPriceList.get(size - 2).getClosePrice();

        double gapOpen = (double) todayOpen / prevClose - 1.0;

        return Map.of(IndicatorType.GAP_OPEN, gapOpen);
    }
}
