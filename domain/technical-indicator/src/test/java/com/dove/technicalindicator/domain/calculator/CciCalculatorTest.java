package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.CciCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CciCalculatorTest {

    private final CciCalculator calculator = new CciCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long high, long low, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 CCI(20)를 검증한다")
    void shouldCalculateCciFromKnownValues() {
        // Given - 20개 데이터, 상승 추세
        List<DailyStockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110 + i, 90 + i, 100 + i))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 상승 추세에서 마지막 TP > SMA(TP)이므로 CCI > 0
        assertThat(result.get(IndicatorType.CCI)).isGreaterThan(0);
    }

    @Test
    @DisplayName("가격이 평균에 있으면 CCI는 0에 가깝다")
    void shouldReturnZeroWhenPriceAtAverage() {
        // Given - 모든 가격이 동일
        List<DailyStockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 100))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.CCI)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("20개 데이터 포인트가 필요하다")
    void shouldRequire20DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(20);
    }
}
