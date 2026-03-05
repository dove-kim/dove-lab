package com.dove.stockdata.domain.entity;

import com.dove.stockdata.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

/**
 * Stock 엔티티의 복합키. 시장 유형과 종목 코드 조합.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockId {
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "CODE", nullable = false, length = 20)
    private String code;
}
