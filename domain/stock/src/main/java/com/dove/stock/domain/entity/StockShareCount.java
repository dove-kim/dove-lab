package com.dove.stock.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상장주식수 변경이력 — 값이 바뀐 날만 한 행. as-of 조회로 특정 거래일의 주식수를 얻는다.
 */
@Getter
@Entity
@Table(name = "STOCK_SHARE_COUNT")
@IdClass(StockShareCountId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockShareCount {

    @Id
    @Column(name = "TICKER", length = 20, nullable = false, updatable = false)
    @Comment("종목코드")
    private String ticker;

    @Id
    @Column(name = "EFFECTIVE_DATE", nullable = false, updatable = false)
    @Comment("이 주식수가 처음 관측된(=변경된) 거래일")
    private LocalDate effectiveDate;

    @Column(name = "LISTED_SHARES", nullable = false)
    @Comment("상장주식수(KRX LIST_SHRS)")
    private Long listedShares;

    @Column(name = "SOURCE", length = 10, nullable = false)
    @Comment("출처(KRX 등)")
    private String source;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StockShareCount(String ticker, LocalDate effectiveDate, Long listedShares, String source) {
        this.ticker = ticker;
        this.effectiveDate = effectiveDate;
        this.listedShares = listedShares;
        this.source = source;
    }
}
