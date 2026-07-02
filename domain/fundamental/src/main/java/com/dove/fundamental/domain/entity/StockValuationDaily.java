package com.dove.fundamental.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 종목·일별 밸류에이션 — 실제(RAW) 종가 × PIT 최신 재무로 산출한 시총·4비율.
 */
@Getter
@Entity
@Table(name = "STOCK_VALUATION_DAILY",
        indexes = {
                @Index(name = "IDX_VAL_DATE_MARKETCAP", columnList = "TRADE_DATE, MARKET_CAP"),
        })
@IdClass(StockValuationDailyId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StockValuationDaily {

    @Id
    @Column(name = "TICKER", length = 20, nullable = false, updatable = false)
    @Comment("종목코드")
    private String ticker;

    @Id
    @Column(name = "TRADE_DATE", nullable = false, updatable = false)
    @Comment("거래일")
    private LocalDate tradeDate;

    @Column(name = "CLOSE_PRICE")
    @Comment("실제(RAW) 종가")
    private Long closePrice;

    @Column(name = "MARKET_CAP")
    @Comment("시가총액(RAW종가×보통주식수)")
    private Long marketCap;

    @Column(name = "PER")
    @Comment("시총/당기순이익")
    private Double per;

    @Column(name = "PBR")
    @Comment("시총/자본총계")
    private Double pbr;

    @Column(name = "PSR")
    @Comment("시총/매출액")
    private Double psr;

    @Column(name = "GPA")
    @Comment("매출총이익/자산총계")
    private Double gpa;

    @Column(name = "FUND_RCEPT_NO", length = 14)
    @Comment("사용한 재무 공시 접수번호(PIT 감사추적)")
    private String fundRceptNo;

    @CreationTimestamp
    @Column(name = "CALCULATED_AT", nullable = false, updatable = false)
    @Comment("계산 일시")
    private LocalDateTime calculatedAt;
}
