package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.WilliamsRCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
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

    private DailyStockPrice createDailyStockPrice(LocalDate date, long high, long low, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 Williams %R를 검증한다")
    void shouldCalculateWilliamsRFromKnownValues() {
        // Given - 14일 고가 120, 저가 80, 종가 100
        // %R = (120 - 100) / (120 - 80) * -100 = -50
        List<DailyStockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createDailyStockPrice(
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
        List<DailyStockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createDailyStockPrice(
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
        List<DailyStockPrice> data = IntStream.range(0, 14)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        120, 80, 80))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.WILLIAMS_R)).isCloseTo(-100.0, within(0.01));
    }
}
