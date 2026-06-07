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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PriceRangeRatioCalculatorTest {

    private final PriceRangeRatioCalculator calculator = new PriceRangeRatioCalculator();

    private StockPrice createStockPrice(LocalDate date, long open, long close, long low, long high) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                open, high, low, close, 1000L, null);
    }

    @Test
    @DisplayName("종가가 고저 범위 중간이면 HIGH_20D_RATIO와 HIGH_52W_RATIO 모두 0.5 근방이다")
    void shouldCalculatePriceRangeRatioCorrectly() {
        // Given - 252개 데이터, 고가 200, 저가 100, 종가 150 (정중앙)
        // HIGH_20D_RATIO = (150 - 100) / (200 - 100) = 0.5
        // HIGH_52W_RATIO = (150 - 100) / (200 - 100) = 0.5
        List<StockPrice> data = IntStream.range(0, 252)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        150L, 150L, 100L, 200L))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.HIGH_20D_RATIO)).isCloseTo(0.5, within(0.001));
        assertThat(result.get(IndicatorType.HIGH_52W_RATIO)).isCloseTo(0.5, within(0.001));
    }

    @Test
    @DisplayName("고가와 저가가 같으면 분모가 0이므로 해당 지표를 0.0으로 반환한다")
    void shouldReturnZeroWhenHighEqualsLow() {
        // Given - 252개 데이터, 고가 == 저가 == 종가 == 100 (분모 0)
        List<StockPrice> data = IntStream.range(0, 252)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        100L, 100L, 100L, 100L))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.HIGH_20D_RATIO)).isEqualTo(0.0);
        assertThat(result.get(IndicatorType.HIGH_52W_RATIO)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("데이터가 requiredDataSize 미만이면 빈 맵을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        // Given - 251개 (252 미만)
        List<StockPrice> data = IntStream.range(0, 251)
                .mapToObj(i -> createStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        150L, 150L, 100L, 200L))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).isEmpty();
    }
}
