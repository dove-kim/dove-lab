package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
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

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice, long volume) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                volume, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 OBV를 검증한다")
    void shouldCalculateObvFromKnownValues() {
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 110, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 3), 105, 500),
                createDailyStockPrice(LocalDate.of(2024, 1, 4), 120, 2000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(2500.0, within(0.01));
    }

    @Test
    @DisplayName("상승일에는 거래량을 더한다")
    void shouldAddVolumeOnUpDay() {
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 110, 5000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(5000.0, within(0.01));
    }

    @Test
    @DisplayName("하락일에는 거래량을 뺀다")
    void shouldSubtractVolumeOnDownDay() {
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 110, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 100, 5000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(-5000.0, within(0.01));
    }

    @Test
    @DisplayName("cursorType()은 OBV를 반환한다")
    void shouldReturnObvAsCursorType() {
        assertThat(calculator.cursorType()).isEqualTo(IndicatorType.OBV);
    }

    @Test
    @DisplayName("seed가 주어지면 seed부터 누적한다")
    void shouldAccumulateFromSeedWhenSeedProvided() {
        List<DailyStockPrice> pool = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100, 3000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 110, 5000));

        Map<IndicatorType, Double> result = calculator.calculateWithSeed(pool, 100.0);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(5100.0, within(0.01));
    }

    @Test
    @DisplayName("기존 calculate() 호출 시 seed=0으로 동작한다")
    void shouldUseZeroSeedWhenCalculateCalledDirectly() {
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 110, 4000));

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(4000.0, within(0.01));
    }

    @Test
    @DisplayName("가격 하락 시 seed에서 volume만큼 감소한다")
    void shouldReturnNegativeObvWhenPriceFalls() {
        List<DailyStockPrice> pool = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 110, 1000),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 100, 3000));

        Map<IndicatorType, Double> result = calculator.calculateWithSeed(pool, 200.0);

        assertThat(result.get(IndicatorType.OBV)).isCloseTo(-2800.0, within(0.01));
    }
}
