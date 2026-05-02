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

class MaRatioCalculatorTest {

    private final MaRatioCalculator calculator = new MaRatioCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("정배열 상태에서 MA5 > MA20이면 MA5_MA20_RATIO는 1보다 크다")
    void shouldCalculateMaRatioCorrectly() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i * 10))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MA5_MA20_RATIO)).isGreaterThan(1.0);
    }

    @Test
    @DisplayName("정배열 상태에서 MA20 > MA60이면 MA20_MA60_RATIO는 1보다 크다")
    void shouldReturnRatioAboveOneWhenMa20AboveMa60() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i * 10))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MA20_MA60_RATIO)).isGreaterThan(1.0);
    }

    @Test
    @DisplayName("가격이 일정하면 모든 이동평균 비율은 1이다")
    void shouldReturnOneWhenAllPricesEqual() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 5000L))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.MA5_MA20_RATIO)).isCloseTo(1.0, within(0.0001));
        assertThat(result.get(IndicatorType.MA20_MA60_RATIO)).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("MA5_MA20_RATIO 수식을 검증한다: sma(5) / sma(20)")
    void shouldMatchFormulaForMa5Ma20Ratio() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        double sma5 = IntStream.range(55, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double sma20 = IntStream.range(40, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double expected = sma5 / sma20;

        assertThat(result.get(IndicatorType.MA5_MA20_RATIO)).isCloseTo(expected, within(0.0001));
    }

    @Test
    @DisplayName("MA20_MA60_RATIO 수식을 검증한다: sma(20) / sma(60)")
    void shouldMatchFormulaForMa20Ma60Ratio() {
        List<DailyStockPrice> data = IntStream.range(0, 60)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000L + i))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        double sma20 = IntStream.range(40, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double sma60 = IntStream.range(0, 60).mapToLong(i -> data.get(i).getClosePrice()).average().orElse(0);
        double expected = sma20 / sma60;

        assertThat(result.get(IndicatorType.MA20_MA60_RATIO)).isCloseTo(expected, within(0.0001));
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
    @DisplayName("getName()은 MA_RATIO를 반환한다")
    void shouldReturnCorrectName() {
        assertThat(calculator.getName()).isEqualTo("MA_RATIO");
    }

    @Test
    @DisplayName("requiredDataSize()는 60을 반환한다")
    void shouldRequire60DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(60);
    }
}
