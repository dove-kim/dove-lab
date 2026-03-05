package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.StochasticCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StochasticCalculatorTest {

    private final StochasticCalculator calculator = new StochasticCalculator();

    private StockData createStockData(LocalDate date, long high, long low, long close) {
        return new StockData(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("14일 고가/저가/종가를 기반으로 %K를 계산한다")
    void shouldCalculatePercentK() {
        // Given - 20개 데이터 (14 + 7 - 1 = 20)
        // 고가 200, 저가 80, 종가가 점진적으로 상승 (고저 범위 안)
        List<StockData> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockData(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        200, 80, 90 + i))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - %K는 0~100 범위
        assertThat(result.get(IndicatorType.STOCHASTIC_K)).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("%D는 %K의 7일 SMA이다")
    void shouldCalculatePercentDAsSmoothedK() {
        // Given
        List<StockData> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockData(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        200, 80, 90 + i))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).containsKeys(IndicatorType.STOCHASTIC_K, IndicatorType.STOCHASTIC_D);
        assertThat(result.get(IndicatorType.STOCHASTIC_D)).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("종가가 최고가일 때 %K는 100이다")
    void shouldReturn100WhenCloseAtHigh() {
        // Given - 종가 = 고가 = 110
        List<StockData> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockData(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 110))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.STOCHASTIC_K)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("종가가 최저가일 때 %K는 0이다")
    void shouldReturn0WhenCloseAtLow() {
        // Given - 종가 = 저가 = 90
        List<StockData> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockData(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 90))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.STOCHASTIC_K)).isCloseTo(0.0, within(0.01));
    }
}
