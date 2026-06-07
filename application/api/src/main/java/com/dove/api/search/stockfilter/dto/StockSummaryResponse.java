package com.dove.api.search.stockfilter.dto;

import com.dove.stock.domain.entity.Stock;

/**
 * 종목 요약 응답.
 *
 * @param marketType 시장 구분
 * @param code       종목 코드
 * @param name       종목명
 */
public record StockSummaryResponse(
        String marketType,
        String code,
        String name
) {
    /**
     * 종목 엔티티와 표시명으로 응답 객체를 생성한다.
     */
    public static StockSummaryResponse from(Stock stock, String name) {
        return new StockSummaryResponse(stock.getMarket().name(), stock.getTicker(), name);
    }
}
