package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.SmaCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
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

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("5일 종가 [100,200,300,400,500]의 SMA는 300.0이다")
    void shouldCalculateSma5FromFiveDataPoints() {
        // Given
        List<DailyStockPrice> data = List.of(
                createDailyStockPrice(LocalDate.of(2024, 1, 1), 100),
                createDailyStockPrice(LocalDate.of(2024, 1, 2), 200),
                createDailyStockPrice(LocalDate.of(2024, 1, 3), 300),
                createDailyStockPrice(LocalDate.of(2024, 1, 4), 400),
                createDailyStockPrice(LocalDate.of(2024, 1, 5), 500));

        // When
        Map<IndicatorType, Double> result = sma5Calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_5)).isCloseTo(300.0, within(0.01));
    }

    @Test
    @DisplayName("getName()은 SMA_5를 반환한다")
    void shouldReturnCorrectIndicatorName() {
        assertThat(sma5Calculator.getName()).isEqualTo("SMA_5");
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
        List<DailyStockPrice> data = List.of(
                new DailyStockPrice(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 1),
                        9999L, 9999L, 100L, 1L, 9999L),
                new DailyStockPrice(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 2),
                        9999L, 9999L, 100L, 1L, 9999L),
                new DailyStockPrice(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 3),
                        9999L, 9999L, 100L, 1L, 9999L),
                new DailyStockPrice(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 4),
                        9999L, 9999L, 100L, 1L, 9999L),
                new DailyStockPrice(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 5),
                        9999L, 9999L, 100L, 1L, 9999L));

        // When
        Map<IndicatorType, Double> result = sma5Calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_5)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("cursorType()은 SMA_5를 반환한다")
    void shouldReturnSma5AsCursorType() {
        assertThat(sma5Calculator.cursorType()).isEqualTo(IndicatorType.SMA_5);
    }

    @Test
    @DisplayName("20일 이동평균을 계산한다")
    void shouldHandleSma20() {
        // Given
        SmaCalculator sma20 = new SmaCalculator(20, IndicatorType.SMA_20);
        List<DailyStockPrice> data = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, i), 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = sma20.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.SMA_20)).isCloseTo(1000.0, within(0.01));
    }
}
