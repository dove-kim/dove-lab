package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.calculator.WilliamsRCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WilliamsRCalculatorTest {

    private final WilliamsRCalculator calculator = new WilliamsRCalculator();

    private StockPrice createStockPrice(LocalDate date, long high, long low, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                100L, high, low, close, 1000L, null);
    }

    @Test
    @DisplayName("알려진 값으로 Williams %R를 검증한다")
    void shouldCalculateWilliamsRFromKnownValues() {
        // Given - 14일 고가 120, 저가 80, 종가 100
        // %R = (120 - 100) / (120 - 80) * -100 = -50
        List<StockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        120, 80, 100))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.WILLIAMS_R)).isCloseTo(-50.0, within(0.01));
    }

    @Test
    @DisplayName("종가가 최고가일 때 %R은 0이다")
    void shouldReturn0WhenCloseAtHigh() {
        // Given
        List<StockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        120, 80, 120))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.WILLIAMS_R)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("종가가 최저가일 때 %R은 -100이다")
    void shouldReturnMinus100WhenCloseAtLow() {
        // Given
        List<StockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        120, 80, 80))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.WILLIAMS_R)).isCloseTo(-100.0, within(0.01));
    }

    @Test
    @DisplayName("indicatorType()은 WILLIAMS_R을 반환한다")
    void shouldReturnWilliamsRAsCursorType() {
        assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.WILLIAMS_R);
    }
}
