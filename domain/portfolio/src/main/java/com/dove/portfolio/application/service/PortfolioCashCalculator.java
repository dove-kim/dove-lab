package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 거래의 통화별 현금 영향을 계산하는 순수 계산기. 통화별 현금잔액·순납입·XIRR 흐름의 원천.
 *
 * <p>fx 환산은 다루지 않는다 — 통화별 값을 그대로 반환하고, 원화 환산은 조회 계층(현재 환율)이 맡는다.
 */
@Service
public class PortfolioCashCalculator {

    /**
     * 거래를 접어 통화별 현금잔액을 계산한다.
     */
    public Map<String, BigDecimal> cashByCurrency(List<PortfolioTransaction> transactions) {
        return cashByCurrency(transactions, List.of());
    }

    /**
     * 거래와 환전을 접어 통화별 현금잔액을 계산한다. 환전은 보낸 통화(+수수료)를 빼고 받은 통화를 더한다.
     */
    public Map<String, BigDecimal> cashByCurrency(List<PortfolioTransaction> transactions,
                                                  List<PortfolioFxConversion> conversions) {
        Map<String, BigDecimal> byCurrency = new LinkedHashMap<>();
        for (PortfolioTransaction t : transactions) {
            add(byCurrency, t.getCurrency(), cashEffect(t));
        }
        for (PortfolioFxConversion c : conversions) {
            add(byCurrency, c.getFromCurrency(), c.getFromAmount().add(BigDecimal.valueOf(c.getFee())).negate());
            add(byCurrency, c.getToCurrency(), c.getToAmount());
        }
        return byCurrency;
    }

    /**
     * 통화별 총 입금액(외부유입).
     */
    public Map<String, BigDecimal> depositsByCurrency(List<PortfolioTransaction> transactions) {
        return sumOfType(transactions, TxType.DEPOSIT);
    }

    /**
     * 통화별 총 출금액(외부유출).
     */
    public Map<String, BigDecimal> withdrawalsByCurrency(List<PortfolioTransaction> transactions) {
        return sumOfType(transactions, TxType.WITHDRAW);
    }

    private Map<String, BigDecimal> sumOfType(List<PortfolioTransaction> transactions, TxType type) {
        Map<String, BigDecimal> byCurrency = new LinkedHashMap<>();
        for (PortfolioTransaction t : transactions) {
            if (t.getType() == type) {
                add(byCurrency, t.getCurrency(), t.getAmount());
            }
        }
        return byCurrency;
    }

    /**
     * 거래 1건이 자기 통화 현금에 미치는 영향(부호 포함).
     */
    private BigDecimal cashEffect(PortfolioTransaction t) {
        BigDecimal amount = t.getAmount();
        BigDecimal fee = BigDecimal.valueOf(t.getFee());
        return switch (t.getType()) {
            case DEPOSIT -> amount;
            case WITHDRAW -> amount.negate();
            case BUY -> amount.add(fee).negate();
            case SELL -> amount.subtract(fee);
            case DIVIDEND, INTEREST -> amount;
        };
    }

    private void add(Map<String, BigDecimal> byCurrency, String currency, BigDecimal delta) {
        byCurrency.merge(currency, delta, BigDecimal::add);
    }
}
