package com.dove.stockdata.entity;

import com.dove.stockdata.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
