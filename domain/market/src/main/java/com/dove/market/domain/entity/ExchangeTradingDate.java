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
 * 거래소별 개장일.
 */
@Getter
@Entity
@Table(name = "EXCHANGE_TRADING_DATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeTradingDate {

    @EmbeddedId
    private ExchangeTradingDateId id;

    @Column(name = "PRICES_SYNCED", nullable = false)
    @Comment("전 종목 주가 수집 완료 여부 (false=수집 미완료)")
    private boolean pricesSynced;

    public ExchangeTradingDate(ExchangeTradingDateId id) {
        this.id = id;
        this.pricesSynced = false;
    }

    /**
     * 해당 날짜 전 종목 주가 수집 완료 처리.
     */
    public void markPricesSynced() {
        this.pricesSynced = true;
    }
}
