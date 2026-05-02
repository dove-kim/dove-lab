package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.MacdCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MacdCalculatorTest {

    private final MacdCalculator macdCalculator = new MacdCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 MACD Line을 검증한다")
    void shouldCalculateMacdLineFromKnownValues() {
        List<DailyStockPrice> data = IntStream.range(0, 130)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        10000 + i * 50))
                .toList();

        Map<IndicatorType, Double> result = macdCalculator.calculate(data);

        assertThat(result.get(IndicatorType.MACD_LINE)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Signal Line을 계산한다")
    void shouldCalculateSignalLine() {
        List<DailyStockPrice> data = IntStream.range(0, 130)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        10000 + i * 50))
                .toList();

        Map<IndicatorType, Double> result = macdCalculator.calculate(data);

        assertThat(result).containsKey(IndicatorType.MACD_SIGNAL);
        assertThat(result.get(IndicatorType.MACD_SIGNAL)).isNotNull();
    }

    @Test
    @DisplayName("Histogram은 MACD Line - Signal Line이다")
    void shouldCalculateHistogram() {
        List<DailyStockPrice> data = IntStream.range(0, 130)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        10000 + i * 50))
                .toList();

        Map<IndicatorType, Double> result = macdCalculator.calculate(data);

        double macdLine = result.get(IndicatorType.MACD_LINE);
        double signal = result.get(IndicatorType.MACD_SIGNAL);
        double histogram = result.get(IndicatorType.MACD_HISTOGRAM);
        assertThat(histogram).isCloseTo(macdLine - signal, within(0.001));
    }

    @Test
    @DisplayName("3개 엔트리(MACD_LINE, SIGNAL, HISTOGRAM)를 반환한다")
    void shouldReturnThreeEntryMap() {
        List<DailyStockPrice> data = IntStream.range(0, 130)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i), 10000))
                .toList();

        Map<IndicatorType, Double> result = macdCalculator.calculate(data);

        assertThat(result).hasSize(3);
        assertThat(result).containsKeys(
                IndicatorType.MACD_LINE,
                IndicatorType.MACD_SIGNAL,
                IndicatorType.MACD_HISTOGRAM);
    }

    @Test
    @DisplayName("130개 데이터 포인트가 필요하다")
    void shouldRequire130DataPoints() {
        assertThat(macdCalculator.requiredDataSize()).isEqualTo(130);
    }

    @Test
    @DisplayName("cursorType()은 MACD_LINE을 반환한다")
    void shouldReturnMacdLineAsCursorType() {
        assertThat(macdCalculator.cursorType()).isEqualTo(IndicatorType.MACD_LINE);
    }
}
