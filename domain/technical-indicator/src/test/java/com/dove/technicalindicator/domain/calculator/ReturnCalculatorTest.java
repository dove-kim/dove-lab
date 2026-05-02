package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReturnCalculatorTest {

    private final ReturnCalculator calculator = new ReturnCalculator();

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

    @Test
    @DisplayName("21개 데이터로 로그 수익률 5종을 정확히 계산한다")
    void shouldCalculateLogReturnsCorrectly() {
        long[] closes = new long[21];
        for (int i = 0; i < 21; i++) {
            closes[i] = 10000 + i * 100L;
        }
        List<DailyStockPrice> data = pricesOf(closes);

        Map<IndicatorType, Double> result = calculator.calculate(data);

        long t = closes[20];
        double expected1d = Math.log((double) closes[20] / closes[19]);
        double expected3d = Math.log((double) closes[20] / closes[17]);
        double expected5d = Math.log((double) closes[20] / closes[15]);
        double expected10d = Math.log((double) closes[20] / closes[10]);
        double expected20d = Math.log((double) closes[20] / closes[0]);

        assertThat(result.get(IndicatorType.RETURN_1D)).isCloseTo(expected1d, within(1e-10));
        assertThat(result.get(IndicatorType.RETURN_3D)).isCloseTo(expected3d, within(1e-10));
        assertThat(result.get(IndicatorType.RETURN_5D)).isCloseTo(expected5d, within(1e-10));
        assertThat(result.get(IndicatorType.RETURN_10D)).isCloseTo(expected10d, within(1e-10));
        assertThat(result.get(IndicatorType.RETURN_20D)).isCloseTo(expected20d, within(1e-10));
    }

    @Test
    @DisplayName("21개 미만 데이터이면 빈 Map을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        List<DailyStockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> price(i, 10000 + i * 100L))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getName()은 RETURN을 반환한다")
    void shouldReturnCorrectName() {
        assertThat(calculator.getName()).isEqualTo("RETURN");
    }

    @Test
    @DisplayName("requiredDataSize()는 21을 반환한다")
    void shouldRequire21DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(21);
    }
}
