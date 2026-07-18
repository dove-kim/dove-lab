package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.PortfolioMarket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 해외 종목의 최신 종가(원통화) — (시장, 티커)당 1행, 매일 덮어쓴다. 국내는 STOCK_PRICE 재사용이라 여기 없음.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_QUOTE",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PORTFOLIO_QUOTE_MARKET_TICKER",
                columnNames = {"MARKET", "TICKER"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", nullable = false, length = 20)
    @Comment("상장 시장")
    private PortfolioMarket market;

    @Column(name = "TICKER", nullable = false, length = 20)
    @Comment("시장 내 종목 코드")
    private String ticker;

    @Column(name = "CLOSE_PRICE", nullable = false, precision = 24, scale = 8)
    @Comment("최신 종가(원통화)")
    private BigDecimal closePrice;

    @Column(name = "AS_OF", nullable = false)
    @Comment("종가 기준 일자")
    private LocalDate asOf;

    @Column(name = "FETCHED_AT", nullable = false)
    @Comment("수집 일시(신선도 확인용)")
    private LocalDateTime fetchedAt;

    /**
     * 종가를 생성한다.
     */
    public static PortfolioQuote create(PortfolioMarket market, String ticker, BigDecimal closePrice) {
        PortfolioQuote q = new PortfolioQuote();
        q.market = market;
        q.ticker = ticker;
        q.closePrice = closePrice;
        q.asOf = LocalDate.now();
        q.fetchedAt = LocalDateTime.now();
        return q;
    }

    /**
     * 종가를 갱신한다.
     */
    public void update(BigDecimal closePrice) {
        this.closePrice = closePrice;
        this.asOf = LocalDate.now();
        this.fetchedAt = LocalDateTime.now();
    }
}
