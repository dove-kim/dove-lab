package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GapOpenCalculatorTest {

    private final GapOpenCalculator calculator = new GapOpenCalculator();

    private StockPrice createStockPrice(LocalDate date, long open, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                open, 120L, 90L, close, 1000L, null);
    }

    @Test
    @DisplayName("시가가 전일 종가보다 높으면 GAP_OPEN은 양수이다")
    void shouldCalculateGapOpenCorrectly() {
        // Given - 전일 종가 100, 당일 시가 110
        // GAP_OPEN = 110 / 100 - 1 = 0.1
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L),
                createStockPrice(LocalDate.of(2024, 1, 2), 110L, 105L)
        );

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.GAP_OPEN)).isCloseTo(0.1, within(0.0001));
        assertThat(result.get(IndicatorType.GAP_OPEN)).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("시가가 전일 종가보다 낮으면 GAP_OPEN은 음수이다")
    void shouldReturnNegativeGapOpenWhenOpenBelowPrevClose() {
        // Given - 전일 종가 100, 당일 시가 90
        // GAP_OPEN = 90 / 100 - 1 = -0.1
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L),
                createStockPrice(LocalDate.of(2024, 1, 2), 90L, 95L)
        );

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.GAP_OPEN)).isCloseTo(-0.1, within(0.0001));
        assertThat(result.get(IndicatorType.GAP_OPEN)).isLessThan(0.0);
    }

    @Test
    @DisplayName("데이터가 requiredDataSize 미만이면 빈 맵을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        // Given - 1개 (2 미만)
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L)
        );

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).isEmpty();
    }
}
