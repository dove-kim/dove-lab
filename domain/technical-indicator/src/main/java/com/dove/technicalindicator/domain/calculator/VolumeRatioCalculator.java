package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * 거래량 비율(VR) 계산기. 20일간 상승일/하락일 거래량 비율로 매수 세력을 판단한다.
 */
public class VolumeRatioCalculator implements TechnicalIndicatorCalculator {

    private static final int PERIOD = 20;
    private static final int REQUIRED_SIZE = PERIOD + 1;

    @Override
    public String getName() {
        return "VOLUME_RATIO";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public IndicatorType cursorType() {
        return IndicatorType.VOLUME_RATIO;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        long upVolume = 0;
        long downVolume = 0;
        long unchangedVolume = 0;

        for (int i = 1; i < dailyStockPriceList.size(); i++) {
            long currentClose = dailyStockPriceList.get(i).getClosePrice();
            long previousClose = dailyStockPriceList.get(i - 1).getClosePrice();
            long volume = dailyStockPriceList.get(i).getVolume();

            if (currentClose > previousClose) {
                upVolume += volume;
            } else if (currentClose < previousClose) {
                downVolume += volume;
            } else {
                unchangedVolume += volume;
            }
        }

        double denominator = downVolume + unchangedVolume / 2.0;
        if (denominator == 0) {
            return Map.of(IndicatorType.VOLUME_RATIO, 0.0);
        }

        double volumeRatio = (upVolume + unchangedVolume / 2.0) / denominator * 100;
        return Map.of(IndicatorType.VOLUME_RATIO, volumeRatio);
    }
}
