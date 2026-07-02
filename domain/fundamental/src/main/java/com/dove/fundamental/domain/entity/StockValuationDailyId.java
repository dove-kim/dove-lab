package com.dove.fundamental.domain.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * STOCK_VALUATION_DAILY 복합 식별자 — 종목코드 + 거래일.
 */
public class StockValuationDailyId implements Serializable {

    private String ticker;
    private LocalDate tradeDate;

    protected StockValuationDailyId() {
    }

    public StockValuationDailyId(String ticker, LocalDate tradeDate) {
        this.ticker = ticker;
        this.tradeDate = tradeDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockValuationDailyId that)) {
            return false;
        }
        return Objects.equals(ticker, that.ticker) && Objects.equals(tradeDate, that.tradeDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, tradeDate);
    }
}
