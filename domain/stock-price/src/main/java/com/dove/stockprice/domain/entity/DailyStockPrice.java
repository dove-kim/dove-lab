package com.dove.stockprice.domain.entity;

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

import java.time.LocalDate;

/**
 * 일별 주가 데이터 엔티티. 종목별 거래일의 시가/종가/고가/저가/거래량을 저장한다.
 */
@Getter
@Entity
@Table(name = "DAILY_STOCK_PRICE",
        indexes = @Index(name = "IDX_DAILY_STOCK_PRICE_MARKET_TRADE_DATE",
                columnList = "MARKET_TYPE, TRADE_DATE"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class DailyStockPrice {
    @EmbeddedId
    private DailyStockPriceId id;

    @Column(name = "VOLUME", nullable = false)
    private Long volume;

    @Column(name = "OPEN_PRICE", nullable = false)
    private Long openPrice;

    @Column(name = "CLOSE_PRICE", nullable = false)
    private Long closePrice;

    @Column(name = "LOW_PRICE", nullable = false)
    private Long lowPrice;

    @Column(name = "HIGH_PRICE", nullable = false)
    private Long highPrice;

    public DailyStockPrice(
            MarketType marketType, String stockCode,
            LocalDate tradeDate, Long volume,
            Long openPrice, Long closePrice, Long lowPrice, Long highPrice
    ) {
        this.id = new DailyStockPriceId(marketType, stockCode, tradeDate);
        this.volume = volume;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.lowPrice = lowPrice;
        this.highPrice = highPrice;
    }
}
