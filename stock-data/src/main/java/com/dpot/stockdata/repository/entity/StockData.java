package com.dpot.stockdata.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "OPEN_PRICE", nullable = false, precision = 19, scale = 2)
    private BigDecimal openPrice; // 시가 (소수점 고려하여 BigDecimal 사용)

    @Column(name = "CLOSE_PRICE", nullable = false, precision = 19, scale = 2)
    private BigDecimal closePrice; // 종가

    @Column(name = "LOW_PRICE", nullable = false, precision = 19, scale = 2)
    private BigDecimal lowPrice; // 저가

    @Column(name = "HIGH_PRICE", nullable = false, precision = 19, scale = 2)
    private BigDecimal highPrice; // 고가

    public StockData(
            String stockCode, LocalDate tradeDate, Long volume,
            BigDecimal openPrice, BigDecimal closePrice, BigDecimal lowPrice, BigDecimal highPrice
    ) {
        this.id = new StockDataId(stockCode, tradeDate);
        this.volume = volume;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.lowPrice = lowPrice;
        this.highPrice = highPrice;
    }
}
