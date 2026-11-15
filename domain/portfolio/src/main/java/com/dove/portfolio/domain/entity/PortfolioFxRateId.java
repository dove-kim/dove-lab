package com.dove.portfolio.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 환율 이력의 식별자(원통화 + 고시일).
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioFxRateId implements Serializable {

    @Column(name = "CURRENCY", length = 10, nullable = false)
    @Comment("원통화 코드(USD 등)")
    private String currency;

    @Column(name = "RATE_DATE", nullable = false)
    @Comment("환율 고시 일자")
    private LocalDate rateDate;

    /**
     * 식별자를 생성한다.
     */
    public PortfolioFxRateId(String currency, LocalDate rateDate) {
        this.currency = currency;
        this.rateDate = rateDate;
    }
}
