package com.dove.indicator.domain.entity;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StockFeatureDaily")
class StockFeatureDailyTest {

    private StockFeatureDaily newRow() {
        return new StockFeatureDaily(
                new StockFeatureDailyId("005930", StockExchange.KOSPI, PriceType.ADJUSTED, LocalDate.of(2026, 7, 13)),
                1, 100L, 110L, 90L, 105L, 1000L, 0L, LocalDateTime.of(2026, 7, 13, 0, 0));
    }

    @Nested
    @DisplayName("set")
    class Set {

        @Test
        @DisplayName("유한값은 해당 지표 컬럼에 저장한다")
        void shouldStoreFiniteValue() {
            StockFeatureDaily row = newRow();

            row.set(IndicatorType.RSI_14, 55.0);

            assertThat(row.toIndicatorMap()).containsEntry(IndicatorType.RSI_14, 55.0);
        }

        @Test
        @DisplayName("null 값은 저장하지 않는다")
        void shouldSkipNull() {
            StockFeatureDaily row = newRow();

            row.set(IndicatorType.RSI_14, null);

            assertThat(row.toIndicatorMap()).doesNotContainKey(IndicatorType.RSI_14);
        }

        @Test
        @DisplayName("NaN·±Infinity는 저장하지 않는다(배치 insert 리터럴 인라인 오류 방지)")
        void shouldSkipNonFinite() {
            StockFeatureDaily row = newRow();

            row.set(IndicatorType.RSI_14, Double.NaN);
            row.set(IndicatorType.MACD_LINE, Double.POSITIVE_INFINITY);
            row.set(IndicatorType.ATR, Double.NEGATIVE_INFINITY);

            assertThat(row.toIndicatorMap())
                    .doesNotContainKeys(IndicatorType.RSI_14, IndicatorType.MACD_LINE, IndicatorType.ATR);
        }

        @Test
        @DisplayName("double가 유한이어도 float 범위를 넘으면(Inf 변환) 저장하지 않는다")
        void shouldSkipWhenOverflowsFloatRange() {
            StockFeatureDaily row = newRow();

            row.set(IndicatorType.SMA_5, 1e40); // (float)1e40 = Infinity

            assertThat(row.toIndicatorMap()).doesNotContainKey(IndicatorType.SMA_5);
        }
    }
}
