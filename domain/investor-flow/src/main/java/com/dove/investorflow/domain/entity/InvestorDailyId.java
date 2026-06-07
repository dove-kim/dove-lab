package com.dove.investorflow.domain.entity;

import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 일별 투자자별 매매동향 복합 식별자 (거래소·종목코드·거래일).
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestorDailyId implements Serializable {

    @Column(name = "EXCHANGE", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private StockExchange exchange;

    @Column(name = "STOCK_CODE", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;
}
