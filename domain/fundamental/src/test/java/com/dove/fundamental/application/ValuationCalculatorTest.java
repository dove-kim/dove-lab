package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * ValuationCalculator 단위 테스트.
 */
class ValuationCalculatorTest {

    private StockFundamental.StockFundamentalBuilder base() {
        return StockFundamental.builder()
                .rceptNo("20230307000123").fsDiv(FinancialStatementDiv.CFS)
                .ticker("005930").corpCode("00126380").fiscalYear((short) 2022)
                .reportCode("11011").rceptDt(LocalDate.of(2023, 3, 7)).amendment(false);
    }

    @Nested
    @DisplayName("compute — 시총·4비율")
    class Compute {

        @Test
        @DisplayName("시총·재무로 PER/PBR/PSR/GPA를 계산한다")
        void shouldComputeRatiosFromMarketCap() {
            StockFundamental f = base()
                    .netIncome(2_000_000_000L)
                    .totalEquity(5_000_000_000L)
                    .revenue(10_000_000_000L)
                    .grossProfit(3_000_000_000L)
                    .totalAsset(6_000_000_000L)
                    .build();

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
            StockFundamental f = base()
                    .grossProfit(3_000_000_000L)
                    .totalAsset(6_000_000_000L)
                    .build();

            Valuation v = ValuationCalculator.compute(null, f);

            assertThat(v.marketCap()).isNull();
            assertThat(v.per()).isNull();
            assertThat(v.pbr()).isNull();
            assertThat(v.gpa()).isCloseTo(0.5, within(1e-9));
        }

        @Test
        @DisplayName("분모가 0이거나 없으면 해당 비율만 null")
        void shouldNullRatioWhenDenominatorMissingOrZero() {
            StockFundamental f = base()
                    .netIncome(0L)          // PER 분모 0
                    .totalEquity(null)      // PBR 분모 없음
                    .revenue(1_000_000L)
                    .build();

            Valuation v = ValuationCalculator.compute(100_000L, f);

            assertThat(v.marketCap()).isEqualTo(100_000L);
            assertThat(v.per()).isNull();
            assertThat(v.pbr()).isNull();
            assertThat(v.psr()).isNotNull();
        }
    }
}
