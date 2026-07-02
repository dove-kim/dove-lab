package com.dove.fundamental.domain.entity;

import com.dove.fundamental.domain.enums.FinancialStatementDiv;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * DART 재무제표 원자료 — 공시(접수번호)·재무구분 단위. 원본1+정정N을 각각 보존.
 */
@Getter
@Entity
@Table(name = "STOCK_FUNDAMENTAL",
        indexes = {
                @Index(name = "IDX_FUND_TICKER_PERIOD", columnList = "TICKER, FISCAL_YEAR, REPORT_CODE"),
                @Index(name = "IDX_FUND_TICKER_RCEPT", columnList = "TICKER, RCEPT_DT"),
        })
@IdClass(StockFundamentalId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StockFundamental {

    @Id
    @Column(name = "RCEPT_NO", length = 14, nullable = false, updatable = false)
    @Comment("DART 접수번호(공시 고유)")
    private String rceptNo;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "FS_DIV", length = 3, nullable = false, updatable = false)
    @Comment("재무구분 CFS(연결)/OFS(별도)")
    private FinancialStatementDiv fsDiv;

    @Column(name = "TICKER", length = 20, nullable = false)
    @Comment("종목코드")
    private String ticker;

    @Column(name = "CORP_CODE", length = 8, nullable = false)
    @Comment("DART 고유번호")
    private String corpCode;

    @Column(name = "FISCAL_YEAR", nullable = false)
    @Comment("회계연도")
    private Short fiscalYear;

    @Column(name = "REPORT_CODE", length = 5, nullable = false)
    @Comment("보고서(11011사업/11012반기/11013·11014분기)")
    private String reportCode;

    @Column(name = "RCEPT_DT", nullable = false)
    @Comment("공시일(PIT 기준)")
    private LocalDate rceptDt;

    @Column(name = "IS_AMENDMENT", nullable = false)
    @Comment("정정 여부")
    private boolean amendment;

    @Column(name = "REVENUE")
    @Comment("매출액(ifrs-full_Revenue)")
    private Long revenue;

    @Column(name = "COST_OF_SALES")
    @Comment("매출원가(ifrs-full_CostOfSales)")
    private Long costOfSales;

    @Column(name = "GROSS_PROFIT")
    @Comment("매출총이익(ifrs-full_GrossProfit)")
    private Long grossProfit;

    @Column(name = "OPERATING_INCOME")
    @Comment("영업이익(dart_OperatingIncomeLoss)")
    private Long operatingIncome;

    @Column(name = "PROFIT_BEFORE_TAX")
    @Comment("법인세차감전순이익")
    private Long profitBeforeTax;

    @Column(name = "NET_INCOME")
    @Comment("당기순이익(ifrs-full_ProfitLoss)")
    private Long netIncome;

    @Column(name = "NET_INCOME_CTRL")
    @Comment("지배기업소유주순이익")
    private Long netIncomeControlling;

    @Column(name = "TOTAL_ASSET")
    @Comment("자산총계(ifrs-full_Assets)")
    private Long totalAsset;

    @Column(name = "CURRENT_ASSET")
    @Comment("유동자산")
    private Long currentAsset;

    @Column(name = "NONCURRENT_ASSET")
    @Comment("비유동자산")
    private Long noncurrentAsset;

    @Column(name = "CASH_AND_EQUIV")
    @Comment("현금및현금성자산")
    private Long cashAndEquivalents;

    @Column(name = "INVENTORIES")
    @Comment("재고자산")
    private Long inventories;

    @Column(name = "TOTAL_LIABILITY")
    @Comment("부채총계(ifrs-full_Liabilities)")
    private Long totalLiability;

    @Column(name = "CURRENT_LIABILITY")
    @Comment("유동부채")
    private Long currentLiability;

    @Column(name = "TOTAL_EQUITY")
    @Comment("자본총계(ifrs-full_Equity)")
    private Long totalEquity;

    @Column(name = "EQUITY_CTRL")
    @Comment("지배기업소유주지분")
    private Long equityControlling;

    @Column(name = "ISSUED_CAPITAL")
    @Comment("자본금(ifrs-full_IssuedCapital)")
    private Long issuedCapital;

    @Column(name = "CF_OPERATING")
    @Comment("영업활동현금흐름")
    private Long cashFlowOperating;

    @Column(name = "CF_INVESTING")
    @Comment("투자활동현금흐름")
    private Long cashFlowInvesting;

    @Column(name = "CF_FINANCING")
    @Comment("재무활동현금흐름")
    private Long cashFlowFinancing;

    @Column(name = "COMMON_SHARES")
    @Comment("보통주 상장주식수(stockTotqySttus)")
    private Long commonShares;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("DB 최초 등록 일시")
    private LocalDateTime createdAt;
}
