package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MFI(Money Flow Index) 계산기. 가격과 거래량을 함께 고려하여 자금 유입/유출을 측정한다.
 */
@Component
public class MfiCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 14;

    @Override
    public int requiredDataSize() {
        return PERIOD + 1;
    }

    @Override
    public IndicatorType indicatorType() {
        return IndicatorType.MFI;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<StockPrice> dailyStockPriceList) {
        double positiveFlow = 0;
        double negativeFlow = 0;

        for (int i = 1; i < dailyStockPriceList.size(); i++) {
            StockPrice current = dailyStockPriceList.get(i);
            StockPrice previous = dailyStockPriceList.get(i - 1);

            double currentTp = (current.getHighPrice() + current.getLowPrice() + current.getClosePrice()) / 3.0;
            double previousTp = (previous.getHighPrice() + previous.getLowPrice() + previous.getClosePrice()) / 3.0;

            double rawMoneyFlow = currentTp * current.getVolume();

            if (currentTp > previousTp) {
                positiveFlow += rawMoneyFlow;
            } else if (currentTp < previousTp) {
                negativeFlow += rawMoneyFlow;
            }
        }

        if (negativeFlow == 0) {
            return Map.of(IndicatorType.MFI, 100.0);
        }

        double moneyFlowRatio = positiveFlow / negativeFlow;
        double mfi = 100.0 - (100.0 / (1.0 + moneyFlowRatio));

        return Map.of(IndicatorType.MFI, mfi);
    }
}
