package com.dove.market.domain.entity;

import com.dove.market.domain.enums.Exchange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 거래소별 거래일 복합 식별자 (거래소·거래일).
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class ExchangeTradingDateId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "EXCHANGE", nullable = false, length = 10)
    private Exchange exchange;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;

    public ExchangeTradingDateId(Exchange exchange, LocalDate tradeDate) {
        this.exchange = exchange;
        this.tradeDate = tradeDate;
    }
}
