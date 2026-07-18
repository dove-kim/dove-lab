package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래의 통화별 현금 영향과 XIRR을 검증한다.
 */
class PortfolioCashCalculatorTest {

    private final PortfolioCashCalculator calculator = new PortfolioCashCalculator();
    private static final LocalDate D = LocalDate.of(2026, 7, 1);

    private PortfolioTransaction tx(TxType type, String currency, long amount, long fee) {
        boolean hasSymbol = type == TxType.BUY || type == TxType.SELL;
        return PortfolioTransaction.create(1L, 10L, type, D, hasSymbol ? "종목" : null,
                currency, null, null, BigDecimal.valueOf(amount), fee, null, null, "tester");
    }

    @Nested
    @DisplayName("통화별 현금")
    class CashByCurrency {
        @Test
        @DisplayName("외화 매수하면 그 통화 현금이 음수가 된다(환전 개념 없음)")
        void shouldGoNegativeOnForeignBuy() {
            Map<String, BigDecimal> cash = calculator.cashByCurrency(List.of(
                    tx(TxType.DEPOSIT, "KRW", 1_000_000, 0),
                    tx(TxType.BUY, "KRW", 300_000, 500),
                    tx(TxType.BUY, "USD", 1_456, 1)));

            assertThat(cash.get("KRW")).isEqualByComparingTo("699500"); // 1,000,000 - (300,000+500)
            assertThat(cash.get("USD")).isEqualByComparingTo("-1457");
        }

        @Test
        @DisplayName("배당·이자는 현금을 늘린다(세후 금액으로 기록)")
        void shouldAddIncome() {
            Map<String, BigDecimal> cash = calculator.cashByCurrency(List.of(
                    tx(TxType.INTEREST, "KRW", 4_230, 0),
                    tx(TxType.DIVIDEND, "USD", 85, 0)));

            assertThat(cash.get("KRW")).isEqualByComparingTo("4230");
            assertThat(cash.get("USD")).isEqualByComparingTo("85");
        }
    }

    @Nested
    @DisplayName("입출금 집계")
    class Flows {
        @Test
        @DisplayName("통화별 입금·출금 합계")
        void shouldSumByCurrency() {
            List<PortfolioTransaction> txns = List.of(
                    tx(TxType.DEPOSIT, "KRW", 1_000_000, 0),
                    tx(TxType.DEPOSIT, "USD", 500, 0),
                    tx(TxType.WITHDRAW, "KRW", 200_000, 0));

            assertThat(calculator.depositsByCurrency(txns).get("KRW")).isEqualByComparingTo("1000000");
            assertThat(calculator.depositsByCurrency(txns).get("USD")).isEqualByComparingTo("500");
            assertThat(calculator.withdrawalsByCurrency(txns).get("KRW")).isEqualByComparingTo("200000");
        }
    }

    @Nested
    @DisplayName("XIRR")
    class Xirrs {
        @Test
        @DisplayName("1년 후 10% 증가면 XIRR은 약 10%")
        void shouldApproximateTenPercent() {
            OptionalDouble rate = Xirr.annualRatePct(
                    List.of(new ExternalFlow(LocalDate.of(2025, 7, 14), -1_000_000L)),
                    LocalDate.of(2026, 7, 14), 1_100_000L);

            assertThat(rate).isPresent();
            assertThat(rate.getAsDouble()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.3));
        }

        @Test
        @DisplayName("납입이 없으면 XIRR은 빈 값")
        void shouldBeEmptyWithoutContributions() {
            assertThat(Xirr.annualRatePct(List.of(), LocalDate.of(2026, 7, 14), 1_000_000L)).isEmpty();
        }
    }
}
