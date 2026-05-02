package com.dove.market.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "MARKET_TRADING_DATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTradingDate {

    @EmbeddedId
    private MarketTradingDateId id;

    @Column(name = "IS_OPEN", nullable = false)
    @Comment("개장 여부 (true=개장, false=휴장)")
    private boolean open;

    public MarketTradingDate(MarketTradingDateId id, boolean open) {
        this.id = id;
        this.open = open;
    }

    public void markOpen() {
        this.open = true;
    }
}
