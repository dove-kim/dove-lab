package com.dove.api.search.stock.dto;

import com.dove.fundamental.domain.entity.StockValuationDaily;

import java.time.LocalDate;

/**
 * 일별 밸류에이션 응답.
 *
 * @param tradeDate  거래일
 * @param closePrice 종가(RAW)
 * @param marketCap  시가총액
 * @param per        시총/당기순이익
 * @param pbr        시총/자본총계
 * @param psr        시총/매출액
 * @param gpa        매출총이익/자산총계
 */
public record ValuationResponse(
        LocalDate tradeDate,
        Long closePrice,
        Long marketCap,
        Double per,
        Double pbr,
        Double psr,
        Double gpa
) {
    public static ValuationResponse from(StockValuationDaily v) {
        return new ValuationResponse(
                v.getTradeDate(), v.getClosePrice(), v.getMarketCap(),
                v.getPer(), v.getPbr(), v.getPsr(), v.getGpa());
    }
}
