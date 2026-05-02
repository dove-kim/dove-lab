package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MaDeviationCalculatorTest {

    private final MaDeviationCalculator calculator = new MaDeviationCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("MA_DEVIATION_20과 MA_DEVIATION_60을 정확히 계산한다")
    void shouldCalculateMaDeviationCorrectly() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MA_DEVIATION_20)).isCloseTo(0.0, within(0.0001));
        assertThat(result.get(IndicatorType.MA_DEVIATION_60)).isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("최신 종가가 이동평균보다 높으면 편차는 양수다")
    void shouldReturnPositiveDeviationWhenCloseAboveSma() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> {
                    long price = (i < 59) ? 1000L : 1200L;
                    return createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), price);
                })
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MA_DEVIATION_20)).isGreaterThan(0.0);
        assertThat(result.get(IndicatorType.MA_DEVIATION_60)).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("MA_DEVIATION_20 수식을 검증한다: close / sma(close,20) - 1")
    void shouldMatchFormulaForMaDeviation20() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        long close = data.get(59).getClosePrice();
        double sma20 = IntStream.range(40, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double expected = (double) close / sma20 - 1.0;

        assertThat(result.get(IndicatorType.MA_DEVIATION_20)).isCloseTo(expected, within(0.0001));
    }

    @Test
    @DisplayName("MA_DEVIATION_60 수식을 검증한다: close / sma(close,60) - 1")
    void shouldMatchFormulaForMaDeviation60() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        long close = data.get(59).getClosePrice();
        double sma60 = IntStream.range(0, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double expected = (double) close / sma60 - 1.0;

        assertThat(result.get(IndicatorType.MA_DEVIATION_60)).isCloseTo(expected, within(0.0001));
    }

    @Test
    @DisplayName("데이터가 부족하면 빈 맵을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        List<DailyStockPrice> data = IntStream.range(0, 10)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getName()은 MA_DEVIATION을 반환한다")
    void shouldReturnCorrectName() {
        assertThat(calculator.getName()).isEqualTo("MA_DEVIATION");
    }

    @Test
    @DisplayName("requiredDataSize()는 60을 반환한다")
    void shouldRequire60DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(60);
    }
}
