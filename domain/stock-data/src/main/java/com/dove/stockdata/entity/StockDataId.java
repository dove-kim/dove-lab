package com.dove.stockdata.entity;

import com.dove.stockdata.enums.MarketType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDataId {
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "STOCK_CODE", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;
}
