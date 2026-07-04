package com.dove.fundamental.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * ValuationCalculator 단위 테스트.
 */
class ValuationCalculatorTest {

    @Nested
    @DisplayName("compute — 시총·4비율")
    class Compute {

        @Test
        @DisplayName("시총·TTM 재무로 PER/PBR/PSR/GPA를 계산한다")
        void shouldComputeRatiosFromMarketCap() {
            TtmFundamental f = new TtmFundamental(
                    10_000_000_000L,   // revenue
                    3_000_000_000L,    // grossProfit
                    2_000_000_000L,    // netIncome(지배주주)
                    5_000_000_000L,    // equity(지배주주지분)
                    6_000_000_000L,    // totalAsset
                    "20230307000123");

            Valuation v = ValuationCalculator.compute(10_000_000_000L, f);

            assertThat(v.marketCap()).isEqualTo(10_000_000_000L);
            assertThat(v.per()).isCloseTo(5.0, within(1e-9));       // 100억/20억
            assertThat(v.pbr()).isCloseTo(2.0, within(1e-9));       // 100억/50억
            assertThat(v.psr()).isCloseTo(1.0, within(1e-9));       // 100억/100억
            assertThat(v.gpa()).isCloseTo(0.5, within(1e-9));       // 30억/60억
        }

        @Test
        @DisplayName("시총이 없으면(주식수 미확보) 주가비율은 null, GPA만 산출한다")
        void shouldReturnGpaOnlyWhenMarketCapMissing() {
            TtmFundamental f = new TtmFundamental(
                    null, 3_000_000_000L, null, null, 6_000_000_000L, "r");

            Valuation v = ValuationCalculator.compute(null, f);

            assertThat(v.marketCap()).isNull();
            assertThat(v.per()).isNull();
            assertThat(v.pbr()).isNull();
            assertThat(v.gpa()).isCloseTo(0.5, within(1e-9));
        }

        @Test
        @DisplayName("분모가 0이거나 없으면 해당 비율만 null")
        void shouldNullRatioWhenDenominatorMissingOrZero() {
            TtmFundamental f = new TtmFundamental(
                    1_000_000L, null, 0L, null, null, "r");

            Valuation v = ValuationCalculator.compute(100_000L, f);

            assertThat(v.marketCap()).isEqualTo(100_000L);
            assertThat(v.per()).isNull();   // netIncome 0
            assertThat(v.pbr()).isNull();   // equity 없음
            assertThat(v.psr()).isNotNull();
        }
    }
}
