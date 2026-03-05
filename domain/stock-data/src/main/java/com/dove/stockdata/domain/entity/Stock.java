package com.dove.stockdata.domain.entity;

import com.dove.stockdata.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목 마스터 엔티티. 시장/종목코드를 기본키로 하고, 종목명을 관리한다.
 */
@Getter
@Entity
@Table(name = "STOCK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Stock {
    @EmbeddedId
    private StockId id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    public Stock(MarketType marketType, String code, String name) {
        this.id = new StockId(marketType, code);
        this.name = name;
    }

    public Stock updateName(String name) {
        this.name = name;
        return this;
    }
}
