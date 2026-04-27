package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.AdxCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
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
        // Given - 강한 상승 추세 (28개 데이터)
        List<DailyStockPrice> data = IntStream.range(0, 28)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000 + i * 20, // 고가 상승
                        900 + i * 20,  // 저가도 상승
                        950 + i * 20)) // 종가도 상승
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 강한 추세에서 ADX > 0
        assertThat(result.get(IndicatorType.ADX)).isGreaterThan(0);
        assertThat(result).containsKeys(IndicatorType.ADX, IndicatorType.PLUS_DI, IndicatorType.MINUS_DI);
    }

    @Test
    @DisplayName("+DI를 계산한다")
    void shouldCalculatePlusDi() {
        // Given - 상승 추세
        List<DailyStockPrice> data = IntStream.range(0, 28)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000 + i * 20, 900 + i * 20, 950 + i * 20))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 상승 추세에서 +DI > -DI
        assertThat(result.get(IndicatorType.PLUS_DI)).isGreaterThan(result.get(IndicatorType.MINUS_DI));
    }

    @Test
    @DisplayName("-DI를 계산한다")
    void shouldCalculateMinusDi() {
        // Given - 하락 추세
        List<DailyStockPrice> data = IntStream.range(0, 28)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        2000 - i * 20, 1900 - i * 20, 1950 - i * 20))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 하락 추세에서 -DI > +DI
        assertThat(result.get(IndicatorType.MINUS_DI)).isGreaterThan(result.get(IndicatorType.PLUS_DI));
    }

    @Test
    @DisplayName("횡보 시장에서 DM=0이면 0으로 처리한다")
    void shouldHandleFlatMarket() {
        // Given - 모든 가격이 동일 (고가=저가=종가)
        List<DailyStockPrice> data = IntStream.range(0, 28)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1000, 1000, 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.ADX)).isCloseTo(0.0, within(0.01));
        assertThat(result.get(IndicatorType.PLUS_DI)).isCloseTo(0.0, within(0.01));
        assertThat(result.get(IndicatorType.MINUS_DI)).isCloseTo(0.0, within(0.01));
    }
}
