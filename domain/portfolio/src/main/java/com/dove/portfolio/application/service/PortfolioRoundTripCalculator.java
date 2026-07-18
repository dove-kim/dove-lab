package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 거래 목록을 접어 라운드트립(진입~청산 한 사이클)별 실현 성과를 계산하는 순수 계산기.
 *
 * <p>이동평균 원가법(원통화 기준). 현재가·환율은 다루지 않는다.
 */
@Service
public class PortfolioRoundTripCalculator {

    private static final int NAT_SCALE = 8;

    /**
     * 매수·매도 거래를 시간순으로 접어 라운드트립 목록을 반환한다(진입일 내림차순).
     *
     * @param transactions 거래 목록
     * @param asOf         미청산 사이클의 보유일수 기준일(보통 오늘)
     */
    public List<PortfolioRoundTrip> fold(List<PortfolioTransaction> transactions, LocalDate asOf) {
        Map<String, RoundTripState> states = new LinkedHashMap<>();
        List<PortfolioRoundTrip> result = new ArrayList<>();

        transactions.stream()
                .filter(t -> t.getSymbol() != null && t.getQuantity() != null)
                .filter(t -> t.getType() == TxType.BUY || t.getType() == TxType.SELL)
                .sorted(Comparator.comparing(PortfolioTransaction::getTradeDate)
                        .thenComparing(PortfolioTransaction::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(t -> apply(states, t, result));

        states.values().stream()
                .filter(s -> s.cycleOpen && s.quantity.signum() > 0)
                .forEach(s -> result.add(closeOut(s, null, asOf)));

        result.sort(Comparator.comparing(PortfolioRoundTrip::entryDate).reversed());
        return result;
    }

    private void apply(Map<String, RoundTripState> states, PortfolioTransaction t, List<PortfolioRoundTrip> result) {
        RoundTripState s = states.computeIfAbsent(t.getAccountId() + " " + t.getSymbol(),
                k -> new RoundTripState(t.getAccountId(), t.getSymbol(), t.getCurrency()));
        BigDecimal qty = t.getQuantity();
        BigDecimal fee = BigDecimal.valueOf(t.getFee());
        if (t.getType() == TxType.BUY) {
            if (!s.cycleOpen) {
                s.openCycle(t.getTradeDate());
            }
            s.quantity = s.quantity.add(qty);
            s.investedNat = s.investedNat.add(t.getAmount()).add(fee);
            s.costBasis = s.costBasis.add(t.getAmount()).add(fee);
            s.buyQty = s.buyQty.add(qty);
            s.buyPxQty = s.buyPxQty.add(qty.multiply(t.getPrice()));
            return;
        }
        // SELL — 잔여 비율로 원가 축소, 실현손익 누적
        BigDecimal sellQty = qty.min(s.quantity);
        if (s.quantity.signum() <= 0 || sellQty.signum() <= 0) {
            return;
        }
        BigDecimal remaining = s.quantity.subtract(sellQty);
        BigDecimal costOfSold = s.investedNat.subtract(NativeAmounts.proportion(s.investedNat, remaining, s.quantity));
        BigDecimal proceedsNet = t.getAmount().subtract(fee);
        s.realizedPnl = s.realizedPnl.add(proceedsNet).subtract(costOfSold);
        s.investedNat = NativeAmounts.proportion(s.investedNat, remaining, s.quantity);
        s.quantity = remaining;
        s.sellQty = s.sellQty.add(sellQty);
        s.sellPxQty = s.sellPxQty.add(sellQty.multiply(t.getPrice()));
        if (s.quantity.signum() == 0) {
            result.add(closeOut(s, t.getTradeDate(), t.getTradeDate()));
            s.cycleOpen = false;
        }
    }

    private PortfolioRoundTrip closeOut(RoundTripState s, LocalDate exitDate, LocalDate holdingEnd) {
        boolean open = exitDate == null;
        BigDecimal avgNat = s.buyQty.signum() > 0
                ? s.buyPxQty.divide(s.buyQty, NAT_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal exitNat = !open && s.sellQty.signum() > 0
                ? s.sellPxQty.divide(s.sellQty, NAT_SCALE, RoundingMode.HALF_UP) : null;
        double pnlPct = s.costBasis.signum() > 0
                ? round2(s.realizedPnl.multiply(BigDecimal.valueOf(100)).divide(s.costBasis, 6, RoundingMode.HALF_UP).doubleValue())
                : 0.0;
        long holdingDays = ChronoUnit.DAYS.between(s.entryDate, holdingEnd);
        return new PortfolioRoundTrip(s.accountId, s.symbol, s.currency, s.entryDate, exitDate,
                holdingDays, avgNat, exitNat, s.realizedPnl, pnlPct, open);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
