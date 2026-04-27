package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import java.util.List;
import java.util.Map;

/**
 * MACD 계산기. 12일/26일 EMA 차이(MACD Line), 9일 시그널, 히스토그램을 산출한다.
 */
public class MacdCalculator implements TechnicalIndicatorCalculator {

    private static final int SHORT_PERIOD = 12;
    private static final int LONG_PERIOD = 26;
    private static final int SIGNAL_PERIOD = 9;
    private static final int REQUIRED_SIZE = LONG_PERIOD + SIGNAL_PERIOD;

    @Override
    public String getName() {
        return "MACD";
    }

    @Override
    public int requiredDataSize() {
        return REQUIRED_SIZE;
    }

    @Override
    public Map<IndicatorType, Double> calculate(List<DailyStockPrice> dailyStockPriceList) {
        double[] closePrices = dailyStockPriceList.stream()
                .mapToDouble(DailyStockPrice::getClosePrice)
                .toArray();

        double[] ema12 = calculateEma(closePrices, SHORT_PERIOD);
        double[] ema26 = calculateEma(closePrices, LONG_PERIOD);

        int macdStartIndex = LONG_PERIOD - 1;
        double[] macdLine = new double[closePrices.length - macdStartIndex];
        for (int i = 0; i < macdLine.length; i++) {
            macdLine[i] = ema12[macdStartIndex + i] - ema26[macdStartIndex + i];
        }

        double[] signalLine = calculateEmaFromArray(macdLine, SIGNAL_PERIOD);

        double currentMacd = macdLine[macdLine.length - 1];
        double currentSignal = signalLine[signalLine.length - 1];
        double histogram = currentMacd - currentSignal;

        return Map.of(
                IndicatorType.MACD_LINE, currentMacd,
                IndicatorType.MACD_SIGNAL, currentSignal,
                IndicatorType.MACD_HISTOGRAM, histogram
        );
    }

    private double[] calculateEma(double[] prices, int period) {
        double[] ema = new double[prices.length];
        double multiplier = 2.0 / (period + 1);

        double sma = 0;
        for (int i = 0; i < period; i++) {
            sma += prices[i];
        }
        sma /= period;
        ema[period - 1] = sma;

        for (int i = period; i < prices.length; i++) {
            ema[i] = (prices[i] - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }

    private double[] calculateEmaFromArray(double[] values, int period) {
        double[] ema = new double[values.length];
        double multiplier = 2.0 / (period + 1);

        double sma = 0;
        for (int i = 0; i < period; i++) {
            sma += values[i];
        }
        sma /= period;
        ema[period - 1] = sma;

        for (int i = period; i < values.length; i++) {
            ema[i] = (values[i] - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }
}
