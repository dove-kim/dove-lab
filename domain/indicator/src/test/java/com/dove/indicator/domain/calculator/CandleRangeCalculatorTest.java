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
import static org.assertj.core.api.Assertions.within;

@DisplayName("CandleRangeCalculator")
class CandleRangeCalculatorTest {

    private final CandleRangeCalculator calculator = new CandleRangeCalculator();

    private StockPrice price(long open, long high, long low, long close) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW,
                LocalDate.of(2024, 1, 1), open, high, low, close, 1000L, null);
    }

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("윗꼬리 비율은 (고가-max(시가,종가))/(고가-저가)이다")
        void shouldComputeUpperWickRatio() {
            // open=100, close=120, high=150, low=90 → range=60, upper=(150-120)/60=0.5
            Map<IndicatorType, Double> result = calculator.calculate(List.of(price(100, 150, 90, 120)));

            assertThat(result.get(IndicatorType.UPPER_WICK_RATIO)).isCloseTo(0.5, within(1e-9));
        }

        @Test
        @DisplayName("종가 위치는 (종가-저가)/(고가-저가)이다")
        void shouldComputeClosePos() {
            // close=120, low=90, high=150 → range=60, pos=(120-90)/60=0.5
            Map<IndicatorType, Double> result = calculator.calculate(List.of(price(100, 150, 90, 120)));

            assertThat(result.get(IndicatorType.CLOSE_POS)).isCloseTo(0.5, within(1e-9));
        }

        @Test
        @DisplayName("종가가 고가와 같으면 종가 위치는 1.0이다")
        void shouldReturnClosePosOneWhenCloseEqualsHigh() {
            Map<IndicatorType, Double> result = calculator.calculate(List.of(price(100, 150, 90, 150)));

            assertThat(result.get(IndicatorType.CLOSE_POS)).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("고가와 저가가 같으면(분모 0) 빈 맵을 반환한다")
        void shouldReturnEmptyWhenRangeIsZero() {
            Map<IndicatorType, Double> result = calculator.calculate(List.of(price(100, 100, 100, 100)));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("주가가 비어 있으면 빈 맵을 반환한다")
        void shouldReturnEmptyWhenNoPrices() {
            assertThat(calculator.calculate(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("메타데이터")
    class Metadata {

        @Test
        @DisplayName("requiredDataSize는 1이다")
        void shouldRequireOnePrice() {
            assertThat(calculator.requiredDataSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("indicatorType은 UPPER_WICK_RATIO이다")
        void shouldReturnUpperWickRatioAsType() {
            assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.UPPER_WICK_RATIO);
        }
    }
}
