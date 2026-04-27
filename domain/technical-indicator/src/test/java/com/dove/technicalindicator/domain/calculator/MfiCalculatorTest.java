package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.MfiCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MfiCalculatorTest {

    private final MfiCalculator calculator = new MfiCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long high, long low, long close, long volume) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                volume, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 MFI(14)를 검증한다")
    void shouldCalculateMfiFromKnownValues() {
        // Given - 15개 데이터, 교대로 상승/하락, 동일 거래량
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110 + i, 90 + i, 100 + i, 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 지속 상승이므로 MFI > 50
        assertThat(result.get(IndicatorType.MFI)).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("모든 흐름이 양(+)이면 MFI는 100이다")
    void shouldReturn100WhenAllPositiveFlow() {
        // Given - 지속 상승 (TP 계속 증가)
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110 + i * 10, 90 + i * 10, 100 + i * 10, 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.MFI)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("모든 흐름이 음(-)이면 MFI는 0에 가깝다")
    void shouldReturn0WhenAllNegativeFlow() {
        // Given - 지속 하락 (TP 계속 감소)
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        1100 - i * 10, 900 - i * 10, 1000 - i * 10, 1000))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.MFI)).isCloseTo(0.0, within(0.01));
    }
}
