package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.PortfolioMarket;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 해외 종가 이력의 식별자(시장 + 티커 + 기준일).
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioQuoteId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", nullable = false, length = 20)
    @Comment("상장 시장")
    private PortfolioMarket market;

    @Column(name = "TICKER", nullable = false, length = 20)
    @Comment("시장 내 종목 코드")
    private String ticker;

    @Column(name = "AS_OF", nullable = false)
    @Comment("종가 기준 일자")
    private LocalDate asOf;

    /**
     * 식별자를 생성한다.
     */
    public PortfolioQuoteId(PortfolioMarket market, String ticker, LocalDate asOf) {
        this.market = market;
        this.ticker = ticker;
        this.asOf = asOf;
    }
}
