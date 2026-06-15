package com.dove.screening.domain.enums;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.StockExchange;

import java.util.List;

/**
 * 검색 필터가 평가에 사용할 지표 데이터의 거래소(venue).
 */
public enum FilterVenue {
    /** KRX 정규 거래소 — 시장별 거래소(KOSPI/KOSDAQ/KONEX) 데이터. */
    KRX,
    /** 대체거래소 NXT — 코스피·코스닥만. */
    NXT,
    /** KRX+NXT 통합 — 코스피·코스닥만. */
    INTEGRATED;

    /**
     * 주어진 시장들을 이 venue로 평가할 때 실제 조회할 거래소 목록을 해석한다.
     */
    public List<StockExchange> resolveExchanges(List<MarketType> markets) {
        return switch (this) {
            case KRX -> markets.stream().map(StockExchange::fromMarket).toList();
            case NXT -> List.of(StockExchange.NXT);
            case INTEGRATED -> List.of(StockExchange.INTEGRATED);
        };
    }
}
