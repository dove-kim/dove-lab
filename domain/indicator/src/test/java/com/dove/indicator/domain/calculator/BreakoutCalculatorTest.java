package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BreakoutCalculator")
class BreakoutCalculatorTest {

    private final BreakoutCalculator calculator = new BreakoutCalculator();

    private StockPrice price(int dayOffset, long high, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW,
                LocalDate.of(2024, 1, 1).plusDays(dayOffset),
                close, high, close, close, 1000L, null);
    }

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("종가가 직전 20일 최고 고가를 넘으면 BREAKOUT_20D는 1.0이다")
        void shouldFlagBreakoutWhenCloseAbovePrev20High() {
            // 직전 20봉 고가 모두 1000, 당일 종가 1100 > 1000
            List<StockPrice> data = new ArrayList<>();
            for (int i = 0; i < 20; i++) data.add(price(i, 1000L, 900L));
            data.add(price(20, 1100L, 1100L));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BREAKOUT_20D)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("당일 고가만 직전 고점을 넘고 종가는 넘지 못하면 BREAKOUT_20D는 0.0이다")
        void shouldNotFlagWhenCloseBelowPrev20High() {
            // 직전 20봉 고가 1000, 당일 고가 1200이지만 종가는 950 < 1000
            List<StockPrice> data = new ArrayList<>();
            for (int i = 0; i < 20; i++) data.add(price(i, 1000L, 900L));
            data.add(price(20, 1200L, 950L));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BREAKOUT_20D)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("당일 고가는 비교에서 제외하고 직전 20봉만 본다")
        void shouldExcludeTodayHighFromComparison() {
            // 직전 20봉 중 최고 고가 1050, 당일 종가 1080 > 1050 → 돌파(당일 고가 1080은 분모 미포함)
            List<StockPrice> data = new ArrayList<>();
            for (int i = 0; i < 19; i++) data.add(price(i, 1000L, 900L));
            data.add(price(19, 1050L, 900L));
            data.add(price(20, 1080L, 1080L));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BREAKOUT_20D)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("데이터가 21개 미만이면 빈 맵을 반환한다")
        void shouldReturnEmptyWhenDataInsufficient() {
            List<StockPrice> data = IntStream.range(0, 20)
                    .mapToObj(i -> price(i, 1000L, 900L))
                    .toList();

            assertThat(calculator.calculate(data)).isEmpty();
        }
    }

    @Nested
    @DisplayName("메타데이터")
    class Metadata {

        @Test
        @DisplayName("requiredDataSize는 21이다")
        void shouldRequire21Prices() {
            assertThat(calculator.requiredDataSize()).isEqualTo(21);
        }

        @Test
        @DisplayName("indicatorType은 BREAKOUT_20D이다")
        void shouldReturnBreakout20dAsType() {
            assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.BREAKOUT_20D);
        }
    }
}
