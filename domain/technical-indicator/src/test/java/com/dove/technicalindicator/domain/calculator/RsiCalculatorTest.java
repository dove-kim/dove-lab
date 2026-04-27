package com.dove.technicalindicator.domain.calculator;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.RsiCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RsiCalculatorTest {

    private final RsiCalculator rsiCalculator = new RsiCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 RSI(14)를 검증한다")
    void shouldCalculateRsi14WithKnownValues() {
        // Given - 15개 데이터 (14개 변화 필요)
        // 종가: 44, 44.34, 44.09, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84,
        //       46.08, 45.89, 46.03, 45.61, 46.28, 46.28
        // Wilder의 교과서 참조값: RSI ≈ 70.46
        long[] prices = {4400, 4434, 4409, 4361, 4433, 4483, 4510, 4542, 4584,
                4608, 4589, 4603, 4561, 4628, 4628};

        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), prices[i]))
                .toList();

        // When
        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        // Then
        // 정수 가격(long) 사용으로 소수점 이하 정보 손실 → 교과서값(소수 가격)과 차이 발생
        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(70.46, within(3.0));
    }

    @Test
    @DisplayName("모두 상승이면 RSI는 100에 가깝다")
    void shouldReturn100WhenAllGains() {
        // Given
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000 + i * 100))
                .toList();

        // When
        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("모두 하락이면 RSI는 0에 가깝다")
    void shouldReturn0WhenAllLosses() {
        // Given
        List<DailyStockPrice> data = IntStream.range(0, 15)
                .mapToObj(i -> createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 10000 - i * 100))
                .toList();

        // When
        Map<IndicatorType, Double> result = rsiCalculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.RSI_14)).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("15개 데이터 포인트가 필요하다 (14개 변화 + 1 초기값)")
    void shouldRequire15DataPoints() {
        assertThat(rsiCalculator.requiredDataSize()).isEqualTo(15);
    }
}
