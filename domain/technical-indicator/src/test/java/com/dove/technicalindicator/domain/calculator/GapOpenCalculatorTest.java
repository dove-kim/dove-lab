package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GapOpenCalculatorTest {

    private final GapOpenCalculator calculator = new GapOpenCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long open, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, open, close, 90L, 120L);
    }

    @Test
    @DisplayName("시가가 전일 종가보다 높으면 GAP_OPEN은 양수이다")
    void shouldCalculateGapOpenCorrectly() {
        // Given - 전일 종가 100, 당일 시가 110
        // GAP_OPEN = 110 / 100 - 1 = 0.1
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 110L, 105L)
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
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 90L, 95L)
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
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100L, 100L)
        );

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).isEmpty();
    }
}
