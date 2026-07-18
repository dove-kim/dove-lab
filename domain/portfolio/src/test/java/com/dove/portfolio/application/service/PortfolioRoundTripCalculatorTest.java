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
 * 거래 fold(라운드트립 청산 성과 계산)를 검증한다.
 */
class PortfolioRoundTripCalculatorTest {

    private final PortfolioRoundTripCalculator calculator = new PortfolioRoundTripCalculator();
    private static final LocalDate AS_OF = LocalDate.of(2026, 7, 15);

    private PortfolioTransaction tx(TxType type, long accountId, String symbol, String currency,
                                    String quantity, String price, long amount, long fee, LocalDate date) {
        return PortfolioTransaction.create(1L, accountId, type, date, symbol, currency,
                quantity == null ? null : new BigDecimal(quantity),
                price == null ? null : new BigDecimal(price),
                BigDecimal.valueOf(amount), fee, null, null, "tester");
    }

    private PortfolioTransaction buy(long accountId, String symbol, String quantity, String price,
                                     long amount, LocalDate date) {
        return tx(TxType.BUY, accountId, symbol, "KRW", quantity, price, amount, 0L, date);
    }

    private PortfolioTransaction sell(long accountId, String symbol, String quantity, String price,
                                      long amount, LocalDate date) {
        return tx(TxType.SELL, accountId, symbol, "KRW", quantity, price, amount, 0L, date);
    }

    @Nested
    @DisplayName("청산 라운드트립")
    class ClosedTrips {
        @Test
        @DisplayName("매수 후 전량 매도는 한 청산 사이클로 방출된다")
        void shouldEmitClosedTrip() {
            List<PortfolioRoundTrip> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    sell(10L, "삼성전자", "10", "80000", 800_000L, LocalDate.of(2026, 7, 5))), AS_OF);

            assertThat(result).hasSize(1);
            PortfolioRoundTrip t = result.get(0);
            assertThat(t.open()).isFalse();
            assertThat(t.entryDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(t.exitDate()).isEqualTo(LocalDate.of(2026, 7, 5));
            assertThat(t.holdingDays()).isEqualTo(4);
            assertThat(t.avgNat()).isEqualByComparingTo("70000");
            assertThat(t.exitNat()).isEqualByComparingTo("80000");
            assertThat(t.pnlNat()).isEqualByComparingTo("100000");
            assertThat(t.pnlPct()).isEqualTo(14.29);
        }

        @Test
        @DisplayName("부분 매도 후 전량 청산은 평균 매도가·누적 실현손익을 낸다")
        void shouldAggregatePartialSellsIntoOneTrip() {
            List<PortfolioRoundTrip> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    sell(10L, "삼성전자", "4", "80000", 320_000L, LocalDate.of(2026, 7, 3)),
                    sell(10L, "삼성전자", "6", "75000", 450_000L, LocalDate.of(2026, 7, 6))), AS_OF);

            assertThat(result).hasSize(1);
            PortfolioRoundTrip t = result.get(0);
            assertThat(t.open()).isFalse();
            assertThat(t.exitDate()).isEqualTo(LocalDate.of(2026, 7, 6));
            // 평균 매도가 = (4×80000 + 6×75000) / 10 = 77000
            assertThat(t.exitNat()).isEqualByComparingTo("77000");
            // 실현손익 = (320000-280000) + (450000-420000) = 40000 + 30000 = 70000
            assertThat(t.pnlNat()).isEqualByComparingTo("70000");
            assertThat(t.pnlPct()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("매도 수수료는 실현손익에서 차감한다")
        void shouldSubtractFee() {
            List<PortfolioRoundTrip> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "10", "70000", 700_000L, LocalDate.of(2026, 7, 1)),
                    tx(TxType.SELL, 10L, "삼성전자", "KRW", "10", "80000", 800_000L, 3_000L,
                            LocalDate.of(2026, 7, 5))), AS_OF);

            // 800,000 - 3,000 - 700,000 = 97,000
            assertThat(result.get(0).pnlNat()).isEqualByComparingTo("97000");
        }
    }

    @Nested
    @DisplayName("미청산 사이클")
    class OpenCycle {
        @Test
        @DisplayName("보유 중인 종목은 open으로 방출된다(청산일·매도가 없음)")
        void shouldEmitOpenCycle() {
            List<PortfolioRoundTrip> result = calculator.fold(List.of(
                    buy(20L, "TSLA", "8", "182", 1_456L, LocalDate.of(2026, 7, 1))), AS_OF);

            assertThat(result).hasSize(1);
            PortfolioRoundTrip t = result.get(0);
            assertThat(t.open()).isTrue();
            assertThat(t.exitDate()).isNull();
            assertThat(t.exitNat()).isNull();
            assertThat(t.avgNat()).isEqualByComparingTo("182");
            assertThat(t.pnlNat()).isEqualByComparingTo("0");
            assertThat(t.holdingDays()).isEqualTo(14);
        }
    }

    @Nested
    @DisplayName("재진입")
    class ReEntry {
        @Test
        @DisplayName("전량 매도 후 재매수하면 청산 1건 + 미청산 1건이 된다")
        void shouldSplitClosedAndReopened() {
            List<PortfolioRoundTrip> result = calculator.fold(List.of(
                    buy(10L, "삼성전자", "10", "70000", 700_000L, LocalDate.of(2026, 5, 1)),
                    sell(10L, "삼성전자", "10", "75000", 750_000L, LocalDate.of(2026, 5, 20)),
                    buy(10L, "삼성전자", "5", "72000", 360_000L, LocalDate.of(2026, 6, 10))), AS_OF);

            assertThat(result).hasSize(2);
            assertThat(result).filteredOn(PortfolioRoundTrip::open).hasSize(1);
            assertThat(result).filteredOn(t -> !t.open()).hasSize(1);
            // 진입일 내림차순 정렬 → 재진입(6/10) 먼저
            assertThat(result.get(0).open()).isTrue();
            assertThat(result.get(0).entryDate()).isEqualTo(LocalDate.of(2026, 6, 10));
            assertThat(result.get(1).open()).isFalse();
            assertThat(result.get(1).pnlNat()).isEqualByComparingTo("50000");
        }
    }
}
