package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.indicator.domain.calculator.AdxCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AdxCalculatorTest {

    private final AdxCalculator calculator = new AdxCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long high, long low, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 ADX를 검증한다")
    void shouldCalculateAdxFromKnownValues() {
        List<DailyStockPrice> data = IntStream.range(0, 100)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000 + i * 20,
                        900 + i * 20,
                        950 + i * 20))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.ADX_14)).isGreaterThan(0);
        assertThat(result).containsKeys(IndicatorType.ADX_14, IndicatorType.PLUS_DI_14, IndicatorType.MINUS_DI_14);
    }

    @Test
    @DisplayName("+DI를 계산한다")
    void shouldCalculatePlusDi() {
        List<DailyStockPrice> data = IntStream.range(0, 100)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000 + i * 20, 900 + i * 20, 950 + i * 20))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.PLUS_DI_14)).isGreaterThan(result.get(IndicatorType.MINUS_DI_14));
    }

    @Test
    @DisplayName("-DI를 계산한다")
    void shouldCalculateMinusDi() {
        List<DailyStockPrice> data = IntStream.range(0, 100)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        2000 - i * 20, 1900 - i * 20, 1950 - i * 20))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MINUS_DI_14)).isGreaterThan(result.get(IndicatorType.PLUS_DI_14));
    }

    @Test
    @DisplayName("횡보 시장에서 DM=0이면 0으로 처리한다")
    void shouldHandleFlatMarket() {
        List<DailyStockPrice> data = IntStream.range(0, 100)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000, 1000, 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.ADX_14)).isCloseTo(0.0, within(0.01));
        assertThat(result.get(IndicatorType.PLUS_DI_14)).isCloseTo(0.0, within(0.01));
        assertThat(result.get(IndicatorType.MINUS_DI_14)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("100개 데이터 포인트가 필요하다")
    void shouldRequire100DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("indicatorType()은 ADX_14를 반환한다")
    void shouldReturnAdxAsCursorType() {
        assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.ADX_14);
    }
}
