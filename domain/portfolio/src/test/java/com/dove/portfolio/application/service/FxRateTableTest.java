package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioFxRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일자별 환율 조회 테이블을 검증한다.
 */
class FxRateTableTest {

    private static PortfolioFxRate rate(String currency, String date, String rate) {
        return PortfolioFxRate.create(currency, new BigDecimal(rate), LocalDate.parse(date));
    }

    @Nested
    @DisplayName("일자 환율 조회")
    class RateOn {
        @Test
        @DisplayName("고시일이 일치하면 그 날 환율을 반환한다")
        void shouldReturnExactWhenDateMatches() {
            FxRateTable table = FxRateTable.of(List.of(
                    rate("USD", "2024-03-01", "1335.62"),
                    rate("USD", "2024-03-04", "1332.23")));

            assertThat(table.rateOn("USD", LocalDate.of(2024, 3, 4))).isEqualByComparingTo("1332.23");
        }

        @Test
        @DisplayName("고시가 없는 날은 그 이전 최신 환율을 반환한다")
        void shouldReturnPreviousWhenDateMissing() {
            FxRateTable table = FxRateTable.of(List.of(
                    rate("USD", "2024-03-01", "1335.62"),
                    rate("USD", "2024-03-04", "1332.23")));

            assertThat(table.rateOn("USD", LocalDate.of(2024, 3, 3))).isEqualByComparingTo("1335.62");
        }

        @Test
        @DisplayName("이력 시작 이전 날짜는 가장 이른 환율로 근사한다")
        void shouldReturnEarliestWhenBeforeHistory() {
            FxRateTable table = FxRateTable.of(List.of(rate("USD", "2024-03-01", "1335.62")));

            assertThat(table.rateOn("USD", LocalDate.of(2020, 1, 1))).isEqualByComparingTo("1335.62");
        }

        @Test
        @DisplayName("원화는 이력과 무관하게 1을 반환한다")
        void shouldReturnOneWhenKrw() {
            FxRateTable table = FxRateTable.of(List.of());

            assertThat(table.rateOn("KRW", LocalDate.of(2024, 3, 4))).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("이력이 없는 통화는 1을 반환한다")
        void shouldReturnOneWhenUnknownCurrency() {
            FxRateTable table = FxRateTable.of(List.of(rate("USD", "2024-03-01", "1335.62")));

            assertThat(table.rateOn("JPY", LocalDate.of(2024, 3, 4))).isEqualByComparingTo("1");
        }
    }

    @Nested
    @DisplayName("최신 환율 조회")
    class Latest {
        @Test
        @DisplayName("통화별 가장 최근 고시 환율을 반환한다")
        void shouldReturnLatestPerCurrency() {
            FxRateTable table = FxRateTable.of(List.of(
                    rate("USD", "2024-03-01", "1335.62"),
                    rate("USD", "2024-03-04", "1332.23"),
                    rate("JPY", "2024-03-04", "8.91")));

            assertThat(table.latest("USD")).isEqualByComparingTo("1332.23");
            assertThat(table.latest("JPY")).isEqualByComparingTo("8.91");
        }
    }
}
