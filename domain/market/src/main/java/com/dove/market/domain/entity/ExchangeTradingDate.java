package com.dove.market.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 거래소별 개장·휴장 날짜.
 */
@Getter
@Entity
@Table(name = "EXCHANGE_TRADING_DATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeTradingDate {

    @EmbeddedId
    private ExchangeTradingDateId id;

    @Column(name = "IS_OPEN", nullable = false)
    @Comment("개장 여부 (true=개장, false=휴장)")
    private boolean open;

    @Column(name = "PRICES_SYNCED", nullable = false)
    @Comment("전 종목 주가 수집 완료 여부 (false=수집 미완료)")
    private boolean pricesSynced;

    public ExchangeTradingDate(ExchangeTradingDateId id, boolean open) {
        this.id = id;
        this.open = open;
        this.pricesSynced = false;
    }

    /**
     * 개장일로 확정한다. closed → open 방향만 허용.
     */
    public void markOpen() {
        this.open = true;
    }

    /**
     * 해당 날짜 전 종목 주가 수집 완료 처리.
     */
    public void markPricesSynced() {
        this.pricesSynced = true;
    }
}
