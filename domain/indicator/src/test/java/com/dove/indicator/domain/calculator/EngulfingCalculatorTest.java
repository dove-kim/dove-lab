package com.dove.indicator.domain.calculator;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EngulfingCalculator")
class EngulfingCalculatorTest {

    private final EngulfingCalculator calculator = new EngulfingCalculator();

    private StockPrice price(int dayOffset, long open, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW,
                LocalDate.of(2024, 1, 1).plusDays(dayOffset),
                open, Math.max(open, close), Math.min(open, close), close, 1000L, null);
    }

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("전일 음봉을 당일 양봉이 감싸면 BULLISH_ENGULFING은 1.0이다")
        void shouldFlagBullishEngulfing() {
            // 전일 음봉: open=120, close=100 / 당일 양봉: open=95, close=125 (open<=prevClose, close>=prevOpen)
            List<StockPrice> data = List.of(price(0, 120, 100), price(1, 95, 125));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BULLISH_ENGULFING)).isEqualTo(1.0);
            assertThat(result.get(IndicatorType.BEARISH_ENGULFING)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("전일 양봉을 당일 음봉이 감싸면 BEARISH_ENGULFING은 1.0이다")
        void shouldFlagBearishEngulfing() {
            // 전일 양봉: open=100, close=120 / 당일 음봉: open=125, close=95 (open>=prevClose, close<=prevOpen)
            List<StockPrice> data = List.of(price(0, 100, 120), price(1, 125, 95));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BEARISH_ENGULFING)).isEqualTo(1.0);
            assertThat(result.get(IndicatorType.BULLISH_ENGULFING)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("당일 양봉이 전일 음봉을 완전히 감싸지 못하면 BULLISH_ENGULFING은 0.0이다")
        void shouldNotFlagWhenNotEngulfing() {
            // 당일 양봉이지만 close=115 < prevOpen=120 → 미장악
            List<StockPrice> data = List.of(price(0, 120, 100), price(1, 95, 115));

            Map<IndicatorType, Double> result = calculator.calculate(data);

            assertThat(result.get(IndicatorType.BULLISH_ENGULFING)).isEqualTo(0.0);
            assertThat(result.get(IndicatorType.BEARISH_ENGULFING)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("데이터가 2개 미만이면 빈 맵을 반환한다")
        void shouldReturnEmptyWhenDataInsufficient() {
            assertThat(calculator.calculate(List.of(price(0, 100, 120)))).isEmpty();
        }
    }

    @Nested
    @DisplayName("메타데이터")
    class Metadata {

        @Test
        @DisplayName("requiredDataSize는 2이다")
        void shouldRequireTwoPrices() {
            assertThat(calculator.requiredDataSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("indicatorType은 BULLISH_ENGULFING이다")
        void shouldReturnBullishEngulfingAsType() {
            assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.BULLISH_ENGULFING);
        }
    }
}
