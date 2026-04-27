package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.BollingerBandsCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BollingerBandsCalculatorTest {

    private final BollingerBandsCalculator calculator = new BollingerBandsCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("Middle Band는 20일 SMA이다")
    void shouldCalculateMiddleBandAsSma20() {
        // Given
        List<DailyStockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, i), 1000 + i * 10))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 평균: (1010+1020+...+1200)/20 = 1105
        double expectedSma = IntStream.rangeClosed(1, 20)
                .mapToDouble(i -> 1000 + i * 10)
                .average()
                .orElse(0);
        assertThat(result.get(IndicatorType.BB_MIDDLE)).isCloseTo(expectedSma, within(0.01));
    }

    @Test
    @DisplayName("Upper Band = Middle + 2 * 표준편차")
    void shouldCalculateUpperBandWithStdDev() {
        // Given
        List<DailyStockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, i), 1000 + i * 10))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        double middle = result.get(IndicatorType.BB_MIDDLE);
        double upper = result.get(IndicatorType.BB_UPPER);
        assertThat(upper).isGreaterThan(middle);
    }

    @Test
    @DisplayName("Lower Band = Middle - 2 * 표준편차")
    void shouldCalculateLowerBandWithStdDev() {
        // Given
        List<DailyStockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, i), 1000 + i * 10))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        double middle = result.get(IndicatorType.BB_MIDDLE);
        double lower = result.get(IndicatorType.BB_LOWER);
        assertThat(lower).isLessThan(middle);

        double upper = result.get(IndicatorType.BB_UPPER);
        assertThat(upper - middle).isCloseTo(middle - lower, within(0.01));
    }

    @Test
    @DisplayName("가격이 일정하면 표준편차=0이므로 upper=middle=lower")
    void shouldHandleConstantPrices() {
        // Given
        List<DailyStockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, i), 5000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.BB_UPPER)).isCloseTo(5000.0, within(0.01));
        assertThat(result.get(IndicatorType.BB_MIDDLE)).isCloseTo(5000.0, within(0.01));
        assertThat(result.get(IndicatorType.BB_LOWER)).isCloseTo(5000.0, within(0.01));
    }
}
