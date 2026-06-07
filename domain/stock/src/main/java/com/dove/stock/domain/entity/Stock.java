package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상장 종목 마스터.
 */
@Getter
@Entity
@Table(name = "STOCK",
        indexes = {
                @Index(name = "IDX_STOCK_ISIN", columnList = "ISIN"),
                @Index(name = "IDX_STOCK_SECUGRP", columnList = "SECUGRP_NM"),
                @Index(name = "IDX_STOCK_STKCERT", columnList = "KIND_STKCERT_TP_NM"),
                @Index(name = "IDX_STOCK_MARKET", columnList = "MARKET"),
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @Column(name = "TICKER", length = 20, nullable = false, updatable = false)
    @Comment("종목코드 (KRX ISU_SRT_CD, 국내 6자리)")
    private String ticker;

    @Column(name = "ISIN", length = 12)
    @Comment("ISIN 코드 (KRX ISU_CD, 12자리)")
    private String isin;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", nullable = false, length = 10)
    @Comment("상장 시장 (KOSPI/KOSDAQ/KONEX)")
    private MarketType market;

    @Column(name = "LISTING_DATE")
    @Comment("최초 상장일")
    private LocalDate listingDate;

    @Column(name = "SECUGRP_NM", length = 40)
    @Comment("KRX 증권그룹명 원문 (주권/ETF/리츠 등)")
    private String secugrpNm;

    @Column(name = "KIND_STKCERT_TP_NM", length = 40)
    @Comment("KRX 주권종류명 원문 (보통주/우선주 등, 비주권은 NULL)")
    private String kindStkCertTpNm;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("DB 최초 등록 일시")
    private LocalDateTime createdAt;

    public Stock(String ticker, String isin, MarketType market, LocalDate listingDate,
                 String secugrpNm, String kindStkCertTpNm) {
        this.ticker = ticker;
        this.isin = isin;
        this.market = market;
        this.listingDate = listingDate;
        this.secugrpNm = secugrpNm;
        this.kindStkCertTpNm = kindStkCertTpNm;
    }

    /**
     * KRX 수집 데이터로 종목 정보를 갱신한다.
     */
    public void updateFromKrx(String isin, LocalDate listingDate,
                              String secugrpNm, String kindStkCertTpNm) {
        this.isin = isin;
        this.listingDate = listingDate;
        this.secugrpNm = secugrpNm;
        this.kindStkCertTpNm = kindStkCertTpNm;
    }
}
