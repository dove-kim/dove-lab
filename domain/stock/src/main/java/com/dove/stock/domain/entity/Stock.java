package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.TradingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "STOCK",
        indexes = @Index(name = "IDX_STOCK_TRADING_STATUS", columnList = "TRADING_STATUS"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Stock {
    @EmbeddedId
    private StockId id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "TRADING_STATUS", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Comment("현재 거래 상태 (ACTIVE/SUSPENDED/DELISTED)")
    private TradingStatus tradingStatus;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("DB 최초 등록 일시")
    private LocalDateTime createdAt;

    @Column(name = "LISTING_DATE", nullable = false, updatable = false)
    @Comment("최초 상장일 (KRX 최초 등록 기준)")
    private LocalDate listingDate;

    public Stock(MarketType marketType, String code, String name, TradingStatus tradingStatus, LocalDate listingDate) {
        this.id = new StockId(marketType, code);
        this.name = name;
        this.tradingStatus = tradingStatus;
        this.listingDate = listingDate;
    }

    public Stock updateName(String name) {
        this.name = name;
        return this;
    }

    public void updateTradingStatus(TradingStatus tradingStatus) {
        this.tradingStatus = tradingStatus;
    }
}
