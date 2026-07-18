package com.dove.portfolio.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 원통화별 최신 원화 환율(평가용) — 통화당 1행, 매일 덮어쓴다.
 */
@Getter
@Entity
@Table(name = "PORTFOLIO_FX_RATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioFxRate {

    @Id
    @Column(name = "CURRENCY", length = 10)
    @Comment("원통화 코드(USD 등)")
    private String currency;

    @Column(name = "RATE", nullable = false, precision = 18, scale = 6)
    @Comment("원통화 1단위당 원화")
    private BigDecimal rate;

    @Column(name = "RATE_DATE", nullable = false)
    @Comment("환율 고시 일자")
    private LocalDate rateDate;

    @Column(name = "FETCHED_AT", nullable = false)
    @Comment("수집 일시(신선도 확인용)")
    private LocalDateTime fetchedAt;

    /**
     * 환율을 생성한다.
     */
    public static PortfolioFxRate create(String currency, BigDecimal rate, LocalDate rateDate) {
        PortfolioFxRate f = new PortfolioFxRate();
        f.currency = currency;
        f.rate = rate;
        f.rateDate = rateDate;
        f.fetchedAt = LocalDateTime.now();
        return f;
    }

    /**
     * 환율을 갱신한다.
     */
    public void update(BigDecimal rate, LocalDate rateDate) {
        this.rate = rate;
        this.rateDate = rateDate;
        this.fetchedAt = LocalDateTime.now();
    }
}
