package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.RsiCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RsiCalculatorTest {

    private final RsiCalculator rsiCalculator = new RsiCalculator(14, IndicatorType.RSI_14);

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 RSI(14)를 검증한다")
    void shouldCalculateRsi14WithKnownValues() {
        long[] prices = {4400, 4434, 4409, 4361, 4433, 4483, 4510, 4542, 4584,
                4608, 4589, 4603, 4561, 4628, 4628};

        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), prices[i]))
                .toList();

        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(70.46, within(3.0));
    }

    @Test
    @DisplayName("모두 상승이면 RSI는 100에 가깝다")
    void shouldReturn100WhenAllGains() {
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000 + i * 100))
                .toList();

        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("모두 하락이면 RSI는 0에 가깝다")
    void shouldReturn0WhenAllLosses() {
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 10000 - i * 100))
                .toList();

        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("Wilder 평활법은 단순 평균과 다른 결과를 낸다")
    void shouldCalculateRsiWithWilderSmoothing() {
        long[] prices = {10000, 10200, 10100, 10300, 10250, 10400, 10350, 10500,
                10450, 10600, 10550, 10700, 10650, 10800, 10750,
                10900, 10850, 11000, 10950, 11100, 11050, 11200,
                11150, 11300, 11250, 11400, 11350, 11500, 11450, 11600};

        List<DailyStockPrice> data = IntStream.range(0, 30)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), prices[i]))
                .toList();

        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        assertThat(result.get(IndicatorType.RSI_14)).isBetween(0.0, 100.0);
        assertThat(result.get(IndicatorType.RSI_14)).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("period별 requiredDataSize가 period × 5이다")
    void shouldReturnCorrectRequiredDataSizeByPeriod() {
        assertThat(new RsiCalculator(9, IndicatorType.RSI_9).requiredDataSize()).isEqualTo(45);
        assertThat(new RsiCalculator(14, IndicatorType.RSI_14).requiredDataSize()).isEqualTo(70);
        assertThat(new RsiCalculator(21, IndicatorType.RSI_21).requiredDataSize()).isEqualTo(105);
    }

    @Test
    @DisplayName("cursorType()은 주입된 indicatorType을 반환한다")
    void shouldReturnRsi14AsCursorType() {
        assertThat(rsiCalculator.cursorType()).isEqualTo(IndicatorType.RSI_14);
        assertThat(new RsiCalculator(9, IndicatorType.RSI_9).cursorType()).isEqualTo(IndicatorType.RSI_9);
        assertThat(new RsiCalculator(21, IndicatorType.RSI_21).cursorType()).isEqualTo(IndicatorType.RSI_21);
    }
}
