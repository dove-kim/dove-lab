package com.dove.stock.domain.entity;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;

import java.time.LocalDate;

/**
 * 종목의 일별 OHLCV 주가.
 */
@Getter
@Entity
@Table(name = "STOCK_PRICE",
        indexes = {
                @Index(name = "IDX_SP_TICKER_DATE", columnList = "TICKER, TRADE_DATE"),
                @Index(name = "IDX_SP_EXCHANGE_DATE", columnList = "EXCHANGE, TRADE_DATE"),
                // 거래일 조회(최근일)·거래소별 그날 전종목 조회 — PRICE_TYPE 포함해 인덱스로 좁힘(대용량 풀스캔 방지)
                @Index(name = "IDX_SP_EXCHANGE_PT_DATE", columnList = "EXCHANGE, PRICE_TYPE, TRADE_DATE"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPrice {

    @EmbeddedId
    private StockPriceId id;

    @Column(name = "OPEN_PRICE")
    @Comment("시가")
    private Long openPrice;

    @Column(name = "HIGH_PRICE")
    @Comment("고가")
    private Long highPrice;

    @Column(name = "LOW_PRICE")
    @Comment("저가")
    private Long lowPrice;

    @Column(name = "CLOSE_PRICE")
    @Comment("종가")
    private Long closePrice;

    @Column(name = "VOLUME")
    @Comment("거래량")
    private Long volume;

    @Column(name = "TURNOVER")
    @Comment("거래대금")
    private Long turnover;

    public StockPrice(String ticker, StockExchange exchange, PriceType priceType, LocalDate tradeDate,
                      Long openPrice, Long highPrice, Long lowPrice, Long closePrice,
                      Long volume, Long turnover) {
        this.id = new StockPriceId(ticker, exchange, priceType, tradeDate);
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.turnover = turnover;
    }

    public String getTicker() { return id.getTicker(); }
    public StockExchange getExchange() { return id.getExchange(); }
    public PriceType getPriceType() { return id.getPriceType(); }
    public LocalDate getTradeDate() { return id.getTradeDate(); }

    /**
     * OHLCV 값을 갱신한다.
     */
    public void update(Long openPrice, Long highPrice, Long lowPrice, Long closePrice,
                       Long volume, Long turnover) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.turnover = turnover;
    }
}
