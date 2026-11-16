package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.PortfolioMarket;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 해외 종목의 일자별 종가 이력 — (시장, 티커, 기준일)당 1행. 국내는 STOCK_PRICE 재사용이라 여기 없음.
 */
@Getter
@Entity
@Table(name = "PORTFOLIO_QUOTE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioQuote {

    @EmbeddedId
    private PortfolioQuoteId id;

    @Column(name = "CLOSE_PRICE", nullable = false, precision = 24, scale = 8)
    @Comment("종가(원통화)")
    private BigDecimal closePrice;

    @Column(name = "FETCHED_AT", nullable = false)
    @Comment("수집 일시(신선도 확인용)")
    private LocalDateTime fetchedAt;

    /**
     * 종가를 생성한다.
     */
    public static PortfolioQuote create(PortfolioMarket market, String ticker, BigDecimal closePrice, LocalDate asOf) {
        PortfolioQuote q = new PortfolioQuote();
        q.id = new PortfolioQuoteId(market, ticker, asOf);
        q.closePrice = closePrice;
        q.fetchedAt = LocalDateTime.now();
        return q;
    }

    /**
     * 같은 기준일의 종가를 덮어쓴다.
     */
    public void update(BigDecimal closePrice) {
        this.closePrice = closePrice;
        this.fetchedAt = LocalDateTime.now();
    }

    public PortfolioMarket getMarket() {
        return id.getMarket();
    }

    public String getTicker() {
        return id.getTicker();
    }

    public LocalDate getAsOf() {
        return id.getAsOf();
    }
}
