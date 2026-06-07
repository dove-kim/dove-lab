package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.calculator.SmaCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SmaCalculatorTest {

    private final SmaCalculator sma5Calculator = new SmaCalculator(5, IndicatorType.SMA_5);

    private StockPrice createStockPrice(LocalDate date, long closePrice) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                100L, 110L, 90L, closePrice, 1000L, null);
    }

    @Test
    @DisplayName("5일 종가 [100,200,300,400,500]의 SMA는 300.0이다")
    void shouldCalculateSma5FromFiveDataPoints() {
        // Given
        List<StockPrice> data = List.of(
                createStockPrice(LocalDate.of(2024, 1, 1), 100),
                createStockPrice(LocalDate.of(2024, 1, 2), 200),
                createStockPrice(LocalDate.of(2024, 1, 3), 300),
                createStockPrice(LocalDate.of(2024, 1, 4), 400),
                createStockPrice(LocalDate.of(2024, 1, 5), 500));

        // When
        Map<IndicatorType, Double> result = sma5Calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_5)).isCloseTo(300.0, within(0.01));
    }

    @Test
    @DisplayName("requiredDataSize()는 period 값을 반환한다")
    void shouldRequireExactPeriodDataPoints() {
        assertThat(sma5Calculator.requiredDataSize()).isEqualTo(5);

        SmaCalculator sma20 = new SmaCalculator(20, IndicatorType.SMA_20);
        assertThat(sma20.requiredDataSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("closePrice만 사용하여 계산한다")
    void shouldUseClosePrice() {
        // Given - openPrice, highPrice, lowPrice가 다르지만 closePrice만 사용
        List<StockPrice> data = List.of(
                new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, LocalDate.of(2024, 1, 1),
                        9999L, 9999L, 1L, 100L, 9999L, null),
                new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, LocalDate.of(2024, 1, 2),
                        9999L, 9999L, 1L, 100L, 9999L, null),
                new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, LocalDate.of(2024, 1, 3),
                        9999L, 9999L, 1L, 100L, 9999L, null),
                new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, LocalDate.of(2024, 1, 4),
                        9999L, 9999L, 1L, 100L, 9999L, null),
                new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, LocalDate.of(2024, 1, 5),
                        9999L, 9999L, 1L, 100L, 9999L, null));

        // When
        Map<IndicatorType, Double> result = sma5Calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_5)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("indicatorType()은 SMA_5를 반환한다")
    void shouldReturnSma5AsCursorType() {
        assertThat(sma5Calculator.indicatorType()).isEqualTo(IndicatorType.SMA_5);
    }

    @Test
    @DisplayName("20일 이동평균을 계산한다")
    void shouldHandleSma20() {
        // Given
        SmaCalculator sma20 = new SmaCalculator(20, IndicatorType.SMA_20);
        List<StockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createStockPrice(LocalDate.of(2024, 1, i), 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = sma20.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_20)).isCloseTo(1000.0, within(0.01));
    }
}
