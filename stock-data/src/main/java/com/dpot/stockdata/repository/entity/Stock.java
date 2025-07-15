package com.dpot.stockdata.repository.entity;

import com.dpot.stockdata.domain.enums.StockMarketType;
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

    public Stock(StockMarketType marketType, String code, String name) {
        this.id = new StockId(marketType, code);
        this.name = name;
    }
}
