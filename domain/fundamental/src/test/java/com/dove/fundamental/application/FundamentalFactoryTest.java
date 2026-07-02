package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FundamentalFactory 단위 테스트.
 */
class FundamentalFactoryTest {

    private StockFundamental build(Map<String, Long> accounts) {
        return FundamentalFactory.fromAccounts(
                "005930", "00126380", (short) 2022, "11011",
                "20230307000123", LocalDate.of(2023, 3, 7), FinancialStatementDiv.CFS, false,
                accounts, 5_969_782_550L);
    }

    @Nested
    @DisplayName("fromAccounts — 표준계정 매핑")
    class FromAccounts {

        @Test
        @DisplayName("표준계정코드를 대응 컬럼으로 매핑한다")
        void shouldMapStandardAccountsToColumns() {
            Map<String, Long> accounts = new HashMap<>();
            accounts.put(StandardAccount.REVENUE, 302_231_360_000_000L);
            accounts.put(StandardAccount.GROSS_PROFIT, 112_189_590_000_000L);
            accounts.put(StandardAccount.NET_INCOME, 55_654_077_000_000L);
            accounts.put(StandardAccount.TOTAL_ASSET, 448_424_507_000_000L);
            accounts.put(StandardAccount.TOTAL_EQUITY, 354_749_604_000_000L);

            StockFundamental f = build(accounts);

            assertThat(f.getRevenue()).isEqualTo(302_231_360_000_000L);
            assertThat(f.getGrossProfit()).isEqualTo(112_189_590_000_000L);
            assertThat(f.getNetIncome()).isEqualTo(55_654_077_000_000L);
            assertThat(f.getTotalAsset()).isEqualTo(448_424_507_000_000L);
            assertThat(f.getTotalEquity()).isEqualTo(354_749_604_000_000L);
            assertThat(f.getCommonShares()).isEqualTo(5_969_782_550L);
            assertThat(f.getTicker()).isEqualTo("005930");
        }

        @Test
        @DisplayName("매출총이익이 없으면 매출−매출원가로 보완한다")
        void shouldFallbackGrossProfitFromRevenueMinusCost() {
            Map<String, Long> accounts = new HashMap<>();
            accounts.put(StandardAccount.REVENUE, 1_000L);
            accounts.put(StandardAccount.COST_OF_SALES, 700L);

            StockFundamental f = build(accounts);

            assertThat(f.getGrossProfit()).isEqualTo(300L);
        }

        @Test
        @DisplayName("매출총이익이 있으면 보완하지 않고 원값을 유지한다")
        void shouldKeepGrossProfitWhenPresent() {
            Map<String, Long> accounts = new HashMap<>();
            accounts.put(StandardAccount.REVENUE, 1_000L);
            accounts.put(StandardAccount.COST_OF_SALES, 700L);
            accounts.put(StandardAccount.GROSS_PROFIT, 250L);

            StockFundamental f = build(accounts);

            assertThat(f.getGrossProfit()).isEqualTo(250L);
        }

        @Test
        @DisplayName("없는 계정은 null 로 둔다")
        void shouldLeaveMissingAccountsNull() {
            StockFundamental f = build(new HashMap<>());

            assertThat(f.getRevenue()).isNull();
            assertThat(f.getGrossProfit()).isNull();
            assertThat(f.getCashFlowOperating()).isNull();
        }
    }
}
