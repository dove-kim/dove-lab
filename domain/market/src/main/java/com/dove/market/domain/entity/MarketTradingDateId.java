package com.dove.market.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class MarketTradingDateId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    private MarketType marketType;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;

    public MarketTradingDateId(MarketType marketType, LocalDate tradeDate) {
        this.marketType = marketType;
        this.tradeDate = tradeDate;
    }
}
