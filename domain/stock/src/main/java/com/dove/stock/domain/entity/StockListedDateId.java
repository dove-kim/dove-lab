package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * StockListedDate 엔티티의 복합키. 시장/종목코드/개장일 조합.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockListedDateId {
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "CODE", nullable = false, length = 20)
    private String code;

    @Column(name = "DATE", nullable = false)
    private LocalDate date;
}
