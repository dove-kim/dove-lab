package com.dove.technicalindicator.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TechnicalIndicator 엔티티의 복합키. 시장/종목/거래일/지표유형 조합.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechnicalIndicatorId implements Serializable {

    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "STOCK_CODE", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "INDICATOR_NAME", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private IndicatorType indicatorName;
}
