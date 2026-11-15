package com.dove.portfolio.domain.entity;

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
 * 원통화별 일자 환율 이력 — (통화, 고시일)당 1행.
 */
@Getter
@Entity
@Table(name = "PORTFOLIO_FX_RATE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioFxRate {

    @EmbeddedId
    private PortfolioFxRateId id;

    @Column(name = "RATE", nullable = false, precision = 18, scale = 6)
    @Comment("원통화 1단위당 원화")
    private BigDecimal rate;

    @Column(name = "FETCHED_AT", nullable = false)
    @Comment("수집 일시(신선도 확인용)")
    private LocalDateTime fetchedAt;

    /**
     * 환율을 생성한다.
     */
    public static PortfolioFxRate create(String currency, BigDecimal rate, LocalDate rateDate) {
        PortfolioFxRate f = new PortfolioFxRate();
        f.id = new PortfolioFxRateId(currency, rateDate);
        f.rate = rate;
        f.fetchedAt = LocalDateTime.now();
        return f;
    }

    /**
     * 같은 고시일의 환율을 덮어쓴다.
     */
    public void update(BigDecimal rate) {
        this.rate = rate;
        this.fetchedAt = LocalDateTime.now();
    }

    public String getCurrency() {
        return id.getCurrency();
    }

    public LocalDate getRateDate() {
        return id.getRateDate();
    }
}
