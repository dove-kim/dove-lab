package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
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

    private StockData createStockData(LocalDate date, long high, long low, long close) {
        return new StockData(MarketType.KOSPI, "005930", date,
                1000L, 100L, close, low, high);
    }

    @Test
    @DisplayName("알려진 값으로 ATR(14)를 검증한다")
    void shouldCalculateAtrFromKnownValues() {
        // Given - 15개 데이터, 고가-저가 = 20 일정
        List<StockData> data = IntStream.range(0, 15)
                .mapToObj(i -> createStockData(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        110, 90, 100))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - TR = 20 일정하므로 ATR = 20
        assertThat(result.get(IndicatorType.ATR)).isCloseTo(20.0, within(0.01));
    }

    @Test
    @DisplayName("15개 데이터 포인트가 필요하다")
    void shouldRequire15DataPoints() {
        assertThat(calculator.requiredDataSize()).isEqualTo(15);
    }

    @Test
    @DisplayName("갭 상승 시 True Range를 정확히 계산한다")
    void shouldHandleGapUp() {
        // Given - 전일 종가 100, 당일 고가 120 저가 115 → TR = max(5, 20, 15) = 20
        List<StockData> data = IntStream.range(0, 15)
                .mapToObj(i -> {
                    if (i == 0) {
                        return createStockData(LocalDate.of(2024, 1, 1), 105, 95, 100);
                    }
                    return createStockData(
                            LocalDate.of(2024, 1, 1).plusDays(i),
                            120, 115, 118);
                })
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 갭 상승이 반영된 ATR > 단순 고저차(5)
        assertThat(result.get(IndicatorType.ATR)).isGreaterThan(5.0);
    }
}
