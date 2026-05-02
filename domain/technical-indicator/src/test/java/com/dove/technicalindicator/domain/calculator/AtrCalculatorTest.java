package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.AtrCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AtrCalculatorTest {

    private final AtrCalculator calculator = new AtrCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long high, long low, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 ATR(14)를 검증한다")
    void shouldCalculateAtrFromKnownValues() {
        List<DailyStockPrice> data = IntStream.range(0, 70)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 100))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.ATR)).isCloseTo(20.0, within(0.01));
    }

    @Test
    @DisplayName("70개 데이터 포인트가 필요하다")
    void shouldRequire70DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(70);
    }

    @Test
    @DisplayName("갭 상승 시 True Range를 정확히 계산한다")
    void shouldHandleGapUp() {
        List<DailyStockPrice> data = IntStream.range(0, 70)
                .mapToObj(i -> {
                    if (i == 0) {
                        return createDailyStockPrice(LocalDate.of(2024, 1, 1), 105, 95, 100);
                    }
                    return createDailyStockPrice(
                            LocalDate.of(2024, 1, 1).plusDays(i),
                            120, 115, 118);
                })
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.ATR)).isGreaterThan(5.0);
    }

    @Test
    @DisplayName("cursorType()은 ATR을 반환한다")
    void shouldReturnAtrAsCursorType() {
        assertThat(calculator.cursorType()).isEqualTo(IndicatorType.ATR);
    }
}
