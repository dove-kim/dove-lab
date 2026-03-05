package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.ObvCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ObvCalculatorTest {

    private final ObvCalculator calculator = new ObvCalculator();

    private StockData createStockData(LocalDate date, long closePrice, long volume) {
        return new StockData(MarketType.KOSPI, "005930", date,
                volume, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 OBV를 검증한다")
    void shouldCalculateObvFromKnownValues() {
        // Given - 상승(+1000), 하락(-500), 상승(+2000) → OBV = 2500
        List<StockData> data = List.of(
                createStockData(LocalDate.of(2024, 1, 1), 100, 1000),
                createStockData(LocalDate.of(2024, 1, 2), 110, 1000),
                createStockData(LocalDate.of(2024, 1, 3), 105, 500),
                createStockData(LocalDate.of(2024, 1, 4), 120, 2000));

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.OBV)).isCloseTo(2500.0, within(0.01));
    }

    @Test
    @DisplayName("상승일에는 거래량을 더한다")
    void shouldAddVolumeOnUpDay() {
        // Given
        List<StockData> data = List.of(
                createStockData(LocalDate.of(2024, 1, 1), 100, 1000),
                createStockData(LocalDate.of(2024, 1, 2), 110, 5000));

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.OBV)).isCloseTo(5000.0, within(0.01));
    }

    @Test
    @DisplayName("하락일에는 거래량을 뺀다")
    void shouldSubtractVolumeOnDownDay() {
        // Given
        List<StockData> data = List.of(
                createStockData(LocalDate.of(2024, 1, 1), 110, 1000),
                createStockData(LocalDate.of(2024, 1, 2), 100, 5000));

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.OBV)).isCloseTo(-5000.0, within(0.01));
    }
}
