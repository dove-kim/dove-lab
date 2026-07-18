package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래 fold(원통화 원가 기준 포지션 계산)를 검증한다.
 */
class PortfolioPositionCalculatorTest {

    private final PortfolioPositionCalculator calculator = new PortfolioPositionCalculator();

    private PortfolioTransaction tx(TxType type, long accountId, String symbol, String currency,
                                    String quantity, String price, long amount, long fee, LocalDate date) {
        return PortfolioTransaction.create(1L, accountId, type, date, symbol, currency,
                quantity == null ? null : new BigDecimal(quantity),
                price == null ? null : new BigDecimal(price),
                BigDecimal.valueOf(amount), fee, null, null, "tester");
    }

    private PortfolioTransaction buy(long accountId, String symbol, String currency, String quantity, String price,
                                     long amount, LocalDate date) {
        return tx(TxType.BUY, accountId, symbol, currency, quantity, price, amount, 0L, date);
    }

    @Nested
    @DisplayName("보유 포지션 계산")
    class OpenPositions {
        @Test
        @DisplayName("단일 매수는 보유 수량·평단·원가가 된다")
        void shouldFoldSingleBuy() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1))));

            assertThat(result).hasSize(1);
            PortfolioPositionCost p = result.get(0);
            assertThat(p.symbol()).isEqualTo("삼성전자");
            assertThat(p.quantity()).isEqualByComparingTo("10");
            assertThat(p.avgPriceNat()).isEqualByComparingTo("70000");
            assertThat(p.investedNat()).isEqualByComparingTo("700000");
            assertThat(p.realizedPnlNat()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("여러 매수는 이동평균 단가로 합쳐진다")
        void shouldAverageMultipleBuys() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "50", "71000", 3_550_000L, LocalDate.of(2026, 5, 1)),
                    buy(10L, "삼성전자", "KRW", "80", "66000", 5_280_000L, LocalDate.of(2026, 6, 1))));

            PortfolioPositionCost p = result.get(0);
            assertThat(p.quantity()).isEqualByComparingTo("130");
            assertThat(p.avgPriceNat()).isEqualByComparingTo("67923.07692308");
            assertThat(p.investedNat()).isEqualByComparingTo("8830000");
        }

        @Test
        @DisplayName("외화 종목은 원통화 단가·원가·통화를 유지한다")
        void shouldKeepNativeCurrency() {
            // USD 매수: 금액도 원통화(8×182=1456 USD)
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(20L, "TSLA", "USD", "8", "182", 1_456L, LocalDate.of(2026, 6, 28))));

            PortfolioPositionCost p = result.get(0);
            assertThat(p.currency()).isEqualTo("USD");
            assertThat(p.avgPriceNat()).isEqualByComparingTo("182");
            assertThat(p.investedNat()).isEqualByComparingTo("1456");
        }

        @Test
        @DisplayName("계좌·종목이 다르면 별도 포지션이다")
        void shouldSeparateByAccountAndSymbol() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    buy(20L, "삼성전자", "KRW", "5", "72000", 360_000L, LocalDate.of(2026, 7, 1))));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PortfolioPositionCost::accountId).containsExactlyInAnyOrder(10L, 20L);
        }
    }

    @Nested
    @DisplayName("매도 반영")
    class Sells {
        @Test
        @DisplayName("부분 매도는 원가를 비례 축소하고 실현손익을 누적한다")
        void shouldReduceCostAndRealizeOnPartialSell() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    tx(TxType.SELL, 10L, "삼성전자", "KRW", "4", "80000", 320_000L, 0L, LocalDate.of(2026, 7, 5))));

            PortfolioPositionCost p = result.get(0);
            assertThat(p.quantity()).isEqualByComparingTo("6");
            assertThat(p.avgPriceNat()).isEqualByComparingTo("70000");
            assertThat(p.investedNat()).isEqualByComparingTo("420000");
            assertThat(p.realizedPnlNat()).isEqualByComparingTo("40000");
        }

        @Test
        @DisplayName("전량 매도한 종목은 보유 목록에서 빠진다")
        void shouldDropFullySoldPosition() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    tx(TxType.SELL, 10L, "삼성전자", "KRW", "10", "75000", 750_000L, 0L, LocalDate.of(2026, 7, 5))));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("매도 수수료는 실현손익에서 차감한다")
        void shouldSubtractFeeFromRealized() {
            List<PortfolioPositionCost> partial = calculator.fold(List.of(
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    tx(TxType.SELL, 10L, "삼성전자", "KRW", "5", "75000", 375_000L, 3_000L,
                            LocalDate.of(2026, 7, 5))));

            // 375,000 - 3,000 - (700,000 * 5/10=350,000) = 22,000
            assertThat(partial.get(0).realizedPnlNat()).isEqualByComparingTo("22000");
        }
    }

    @Nested
    @DisplayName("현금 흐름 무시")
    class CashFlows {
        @Test
        @DisplayName("입금·배당 등 종목 없는 거래는 포지션을 만들지 않는다")
        void shouldIgnoreNonTradeTransactions() {
            List<PortfolioPositionCost> result = calculator.fold(List.of(
                    tx(TxType.DEPOSIT, 10L, null, "KRW", null, null, 2_000_000L, 0L, LocalDate.of(2026, 7, 1)),
                    tx(TxType.DIVIDEND, 10L, "SPY", "USD", null, null, 28_900L, 0L, LocalDate.of(2026, 7, 5)),
                    buy(10L, "삼성전자", "KRW", "10", "70000", 700_000L, LocalDate.of(2026, 7, 6))));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).symbol()).isEqualTo("삼성전자");
        }
    }
}
