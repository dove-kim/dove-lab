package com.dove.stockdata.domain.entity;

import com.dove.stockdata.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * StockDataChange의 복합키. 시장/종목/거래일 조합으로 유일성을 보장한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDataChangeId implements Serializable {
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "STOCK_CODE", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;
}
