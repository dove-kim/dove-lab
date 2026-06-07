package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.calculator.StochasticCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
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

    private StockPrice createStockPrice(LocalDate date, long high, long low, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                100L, high, low, close, 1000L, null);
    }

    @Test
    @DisplayName("14일 고가/저가/종가를 기반으로 %K를 계산한다")
    void shouldCalculatePercentK() {
        // Given - 20개 데이터 (14 + 7 - 1 = 20)
        // 고가 200, 저가 80, 종가가 점진적으로 상승 (고저 범위 안)
        List<StockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        200, 80, 90 + i))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - %K는 0~100 범위
        assertThat(result.get(IndicatorType.STOCHASTIC_K_14_7)).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("%D는 %K의 7일 SMA이다")
    void shouldCalculatePercentDAsSmoothedK() {
        // Given
        List<StockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        200, 80, 90 + i))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).containsKeys(IndicatorType.STOCHASTIC_K_14_7, IndicatorType.STOCHASTIC_D_14_7);
        assertThat(result.get(IndicatorType.STOCHASTIC_D_14_7)).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("종가가 최고가일 때 %K는 100이다")
    void shouldReturn100WhenCloseAtHigh() {
        // Given - 종가 = 고가 = 110
        List<StockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 110))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.STOCHASTIC_K_14_7)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("종가가 최저가일 때 %K는 0이다")
    void shouldReturn0WhenCloseAtLow() {
        // Given - 종가 = 저가 = 90
        List<StockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 90))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.STOCHASTIC_K_14_7)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("indicatorType()은 STOCHASTIC_K_14_7를 반환한다")
    void shouldReturnStochasticKAsCursorType() {
        assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.STOCHASTIC_K_14_7);
    }
}
