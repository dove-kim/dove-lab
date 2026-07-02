package com.dove.stock.domain.entity;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 상장주식수 변경이력 복합키(종목코드 + 발효일).
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockShareCountId implements Serializable {

    private String ticker;
    private LocalDate effectiveDate;

    public StockShareCountId(String ticker, LocalDate effectiveDate) {
        this.ticker = ticker;
        this.effectiveDate = effectiveDate;
    }
}
