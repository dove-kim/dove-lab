package com.dove.indicator.domain.breadth.entity;

import com.dove.stock.domain.converter.PriceTypeCodeConverter;
import com.dove.stock.domain.converter.StockExchangeCodeConverter;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * StockBreadthDaily 복합키 — (거래소, 가격유형, 거래일).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockBreadthDailyId implements Serializable {

    @Convert(converter = StockExchangeCodeConverter.class)
    @Column(name = "EXCHANGE", nullable = false)
    private StockExchange exchange;

    @Convert(converter = PriceTypeCodeConverter.class)
    @Column(name = "PRICE_TYPE", nullable = false)
    private PriceType priceType;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;
}
