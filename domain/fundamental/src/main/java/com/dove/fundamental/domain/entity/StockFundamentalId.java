package com.dove.fundamental.domain.entity;

import com.dove.fundamental.domain.enums.FinancialStatementDiv;

import java.io.Serializable;
import java.util.Objects;

/**
 * STOCK_FUNDAMENTAL 복합 식별자 — 접수번호 + 재무구분.
 */
public class StockFundamentalId implements Serializable {

    private String rceptNo;
    private FinancialStatementDiv fsDiv;

    protected StockFundamentalId() {
    }

    public StockFundamentalId(String rceptNo, FinancialStatementDiv fsDiv) {
        this.rceptNo = rceptNo;
        this.fsDiv = fsDiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockFundamentalId that)) {
            return false;
        }
        return Objects.equals(rceptNo, that.rceptNo) && fsDiv == that.fsDiv;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rceptNo, fsDiv);
    }
}
