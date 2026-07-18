package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 거래 목록을 접어 (계좌, 종목)별 원가 기준 포지션을 계산하는 순수 계산기.
 *
 * <p>이동평균 원가법(원통화 기준). 현재가·환율은 다루지 않는다(원통화 원가·수량·실현손익만).
 */
@Service
public class PortfolioPositionCalculator {

    private static final int NAT_SCALE = 8;


    /**
     * 매수·매도 거래를 시간순으로 접어 잔여 보유(수량 &gt; 0) 포지션을 반환한다.
     */
    public List<PortfolioPositionCost> fold(List<PortfolioTransaction> transactions) {
        Map<String, PositionState> states = new LinkedHashMap<>();

        transactions.stream()
                .filter(t -> t.getSymbol() != null && t.getQuantity() != null)
                .filter(t -> t.getType() == TxType.BUY || t.getType() == TxType.SELL)
                .sorted(Comparator.comparing(PortfolioTransaction::getTradeDate)
                        .thenComparing(PortfolioTransaction::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(t -> apply(states, t));

        return states.values().stream()
                .filter(s -> s.quantity.signum() > 0)
                .map(s -> new PortfolioPositionCost(s.accountId, s.symbol, s.currency,
                        s.quantity, s.costNat.divide(s.quantity, NAT_SCALE, RoundingMode.HALF_UP),
                        s.investedNat, s.realizedPnlNat))
                .toList();
    }

    private void apply(Map<String, PositionState> states, PortfolioTransaction t) {
        PositionState s = states.computeIfAbsent(t.getAccountId() + " " + t.getSymbol(),
                k -> new PositionState(t.getAccountId(), t.getSymbol(), t.getCurrency()));
        BigDecimal qty = t.getQuantity();
        BigDecimal fee = BigDecimal.valueOf(t.getFee());
        if (t.getType() == TxType.BUY) {
            s.quantity = s.quantity.add(qty);
            s.costNat = s.costNat.add(qty.multiply(t.getPrice()));
            s.investedNat = s.investedNat.add(t.getAmount()).add(fee);
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
        s.realizedPnlNat = s.realizedPnlNat.add(proceedsNet).subtract(costOfSold);
        s.costNat = NativeAmounts.proportion(s.costNat, remaining, s.quantity);
        s.investedNat = NativeAmounts.proportion(s.investedNat, remaining, s.quantity);
        s.quantity = remaining;
    }
}
