package com.dove.investorflow.domain.entity;

import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

/**
 * 일별 투자자별 매매동향. KIS FHKST01010900 (투자자매매동향) 기준.
 */
@Getter
@Entity
@Table(name = "INVESTOR_DAILY",
        indexes = @Index(name = "IDX_INVESTOR_DAILY_SOURCE_DATE",
                columnList = "EXCHANGE, TRADE_DATE"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestorDaily {

    @EmbeddedId
    private InvestorDailyId id;

    @Column(name = "INDIVIDUAL_BUY", nullable = false)
    @Comment("개인 매수 수량")
    private Long individualBuy;

    @Column(name = "INDIVIDUAL_SELL", nullable = false)
    @Comment("개인 매도 수량")
    private Long individualSell;

    @Column(name = "INSTITUTION_BUY", nullable = false)
    @Comment("기관 매수 수량")
    private Long institutionBuy;

    @Column(name = "INSTITUTION_SELL", nullable = false)
    @Comment("기관 매도 수량")
    private Long institutionSell;

    @Column(name = "FOREIGN_BUY", nullable = false)
    @Comment("외국인 매수 수량")
    private Long foreignBuy;

    @Column(name = "FOREIGN_SELL", nullable = false)
    @Comment("외국인 매도 수량")
    private Long foreignSell;

    public InvestorDaily(StockExchange exchange, String stockCode, LocalDate tradeDate,
                         Long individualBuy, Long individualSell,
                         Long institutionBuy, Long institutionSell,
                         Long foreignBuy, Long foreignSell) {
        this.id = new InvestorDailyId(exchange, stockCode, tradeDate);
        this.individualBuy = individualBuy;
        this.individualSell = individualSell;
        this.institutionBuy = institutionBuy;
        this.institutionSell = institutionSell;
        this.foreignBuy = foreignBuy;
        this.foreignSell = foreignSell;
    }

    public StockExchange getExchange() { return id.getExchange(); }
    public String getStockCode()        { return id.getStockCode(); }
    public LocalDate getTradeDate()     { return id.getTradeDate(); }

    public long individualNet() { return individualBuy - individualSell; }
    public long institutionNet() { return institutionBuy - institutionSell; }
    public long foreignNet()     { return foreignBuy - foreignSell; }
}
