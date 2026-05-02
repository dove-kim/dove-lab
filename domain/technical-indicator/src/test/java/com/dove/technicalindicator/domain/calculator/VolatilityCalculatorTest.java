package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VolatilityCalculatorTest {

    private final VolatilityCalculator calculator = new VolatilityCalculator();

    private DailyStockPrice price(int dayOffset, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930",
                LocalDate.of(2024, 1, 1).plusDays(dayOffset),
                1000L, 100L, closePrice, 90L, 110L);
    }

    private List<DailyStockPrice> pricesOf(long... closePrices) {
        List<DailyStockPrice> list = new ArrayList<>();
        for (int i = 0; i < closePrices.length; i++) {
            list.add(price(i, closePrices[i]));
        }
        return list;
    }

    private double sampleStdDev(double[] values) {
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.length;
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / (values.length - 1));
    }

    @Test
    @DisplayName("21개 데이터로 VOLATILITY_5D와 VOLATILITY_20D를 정확히 계산한다")
    void shouldCalculateVolatilityCorrectly() {
        long[] closes = new long[21];
        for (int i = 0; i < 21; i++) {
            closes[i] = 10000 + (long) (Math.sin(i * 0.5) * 500);
        }
        List<DailyStockPrice> data = pricesOf(closes);

        var result = calculator.calculate(data);

        double[] allReturns = new double[20];
        for (int i = 0; i < 20; i++) {
            allReturns[i] = Math.log((double) closes[i + 1] / closes[i]);
        }
        double[] last5Returns = new double[5];
        System.arraycopy(allReturns, 15, last5Returns, 0, 5);

        double expectedVol5d = sampleStdDev(last5Returns);
        double expectedVol20d = sampleStdDev(allReturns);

        assertThat(result.get(IndicatorType.VOLATILITY_5D)).isCloseTo(expectedVol5d, within(1e-10));
        assertThat(result.get(IndicatorType.VOLATILITY_20D)).isCloseTo(expectedVol20d, within(1e-10));
    }

    @Test
    @DisplayName("21개 미만 데이터이면 빈 Map을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        List<DailyStockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> price(i, 10000 + i * 100L))
                .toList();

        var result = calculator.calculate(data);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getName()은 VOLATILITY를 반환한다")
    void shouldReturnCorrectName() {
        assertThat(calculator.getName()).isEqualTo("VOLATILITY");
    }

    @Test
    @DisplayName("requiredDataSize()는 21을 반환한다")
    void shouldRequire21DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(21);
    }
}
