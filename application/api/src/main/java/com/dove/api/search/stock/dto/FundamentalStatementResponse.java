package com.dove.api.search.stock.dto;

import com.dove.fundamental.domain.entity.StockFundamental;

import java.time.LocalDate;

/**
 * 재무제표 한 건(공시 단위) 응답.
 *
 * @param fiscalYear       회계연도
 * @param reportCode       보고서 코드(11011 사업 등)
 * @param fsDiv            재무구분(CFS/OFS)
 * @param rceptDt          공시일
 * @param amendment        정정 여부
 * @param revenue          매출액
 * @param grossProfit      매출총이익
 * @param operatingIncome  영업이익
 * @param netIncome        당기순이익
 * @param totalAsset       자산총계
 * @param totalLiability   부채총계
 * @param totalEquity      자본총계
 * @param cashFlowOperating 영업활동현금흐름
 */
public record FundamentalStatementResponse(
        Short fiscalYear,
        String reportCode,
        String fsDiv,
        LocalDate rceptDt,
        boolean amendment,
        Long revenue,
        Long grossProfit,
        Long operatingIncome,
        Long netIncome,
        Long totalAsset,
        Long totalLiability,
        Long totalEquity,
        Long cashFlowOperating
) {
    public static FundamentalStatementResponse from(StockFundamental f) {
        return new FundamentalStatementResponse(
                f.getFiscalYear(), f.getReportCode(), f.getFsDiv().name(), f.getRceptDt(), f.isAmendment(),
                f.getRevenue(), f.getGrossProfit(), f.getOperatingIncome(), f.getNetIncome(),
                f.getTotalAsset(), f.getTotalLiability(), f.getTotalEquity(), f.getCashFlowOperating());
    }
}
