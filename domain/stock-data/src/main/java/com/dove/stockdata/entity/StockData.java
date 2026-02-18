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

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "STOCK_DATA")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StockData {
    @EmbeddedId
    private StockDataId id; // 복합 기본 키

    @Column(name = "VOLUME", nullable = false)
    private Long volume; // 거래량

    @Column(name = "OPEN_PRICE", nullable = false)
    private Long openPrice; // 시가

    @Column(name = "CLOSE_PRICE", nullable = false)
    private Long closePrice; // 종가

    @Column(name = "LOW_PRICE", nullable = false)
    private Long lowPrice; // 저가

    @Column(name = "HIGH_PRICE", nullable = false)
    private Long highPrice; // 고가

    public StockData(
            MarketType marketType, String stockCode,
            LocalDate tradeDate, Long volume,
            Long openPrice, Long closePrice, Long lowPrice, Long highPrice
    ) {
        this.id = new StockDataId(marketType, stockCode, tradeDate);
        this.volume = volume;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.lowPrice = lowPrice;
        this.highPrice = highPrice;
    }
}
