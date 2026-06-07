package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.calculator.ObvCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ObvCalculatorTest {

    private final ObvCalculator calculator = new ObvCalculator();

    private StockPrice createStockPrice(LocalDate date, long closePrice, long volume) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                100L, 110L, 90L, closePrice, volume, null);
    }

    @Test
    @DisplayName("알려진 값으로 OBV를 검증한다")
    void shouldCalculateObvFromKnownValues() {
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createStockPrice(LocalDate.of(2024, 1, 2), 110, 1000),
                createStockPrice(LocalDate.of(2024, 1, 3), 105, 500),
                createStockPrice(LocalDate.of(2024, 1, 4), 120, 2000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(2500.0, within(0.01));
    }

    @Test
    @DisplayName("상승일에는 거래량을 더한다")
    void shouldAddVolumeOnUpDay() {
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createStockPrice(LocalDate.of(2024, 1, 2), 110, 5000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(5000.0, within(0.01));
    }

    @Test
    @DisplayName("하락일에는 거래량을 뺀다")
    void shouldSubtractVolumeOnDownDay() {
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 110, 1000),
                createStockPrice(LocalDate.of(2024, 1, 2), 100, 5000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(-5000.0, within(0.01));
    }

    @Test
    @DisplayName("indicatorType()은 OBV를 반환한다")
    void shouldReturnObvAsCursorType() {
        assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.OBV);
    }

    @Test
    @DisplayName("seed가 주어지면 seed부터 누적한다")
    void shouldAccumulateFromSeedWhenSeedProvided() {
        List<StockPrice> pool = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100, 3000),
                createStockPrice(LocalDate.of(2024, 1, 2), 110, 5000));

        Map<IndicatorType, Double> result = calculator.calculateWithSeed(pool, 100.0);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(5100.0, within(0.01));
    }

    @Test
    @DisplayName("기존 calculate() 호출 시 seed=0으로 동작한다")
    void shouldUseZeroSeedWhenCalculateCalledDirectly() {
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createStockPrice(LocalDate.of(2024, 1, 2), 110, 4000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(4000.0, within(0.01));
    }

    @Test
    @DisplayName("가격 하락 시 seed에서 volume만큼 감소한다")
    void shouldReturnNegativeObvWhenPriceFalls() {
        List<StockPrice> pool = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 110, 1000),
                createStockPrice(LocalDate.of(2024, 1, 2), 100, 3000));

        Map<IndicatorType, Double> result = calculator.calculateWithSeed(pool, 200.0);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(-2800.0, within(0.01));
    }
}
