package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
        indexes = @Index(name = "IDX_STOCK_ISIN_CODE", columnList = "ISIN_CODE"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Stock {
    @EmbeddedId
    private StockId id;


    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "ISIN_CODE", length = 12)
    @Comment("ISIN 코드 (KR + 종류코드 1자 + 티커 6자 + 체크디짓 3자 = 12자)")
    private String isinCode;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("DB 최초 등록 일시")
    private LocalDateTime createdAt;

    @Column(name = "LISTING_DATE", nullable = false, updatable = false)
    @Comment("최초 상장일 (KRX 기준)")
    private LocalDate listingDate;

    public Stock(MarketType marketType, String code, String name, LocalDate listingDate, String isinCode) {
        this.id = new StockId(marketType, code);
        this.name = name;
        this.listingDate = listingDate;
        this.isinCode = isinCode;
    }

    public Stock updateName(String name) {
        this.name = name;
        return this;
    }
}
