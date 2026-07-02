package com.dove.stock.domain.enums;

import java.util.List;

/**
 * 횡단면 순위·상승비율 계산을 위해 거래소들을 풀링한 모집단.
 */
public enum MarketUniverse {

    /**
     * 정규장 풀링(KOSPI∪KOSDAQ).
     */
    KRX(List.of(StockExchange.KOSPI, StockExchange.KOSDAQ)),

    /**
     * KONEX 단독.
     */
    KONEX(List.of(StockExchange.KONEX)),

    /**
     * NXT 단독.
     */
    NXT(List.of(StockExchange.NXT)),

    /**
     * 통합(KRX+NXT) 단독.
     */
    INTEGRATED(List.of(StockExchange.INTEGRATED));

    private final List<StockExchange> members;

    MarketUniverse(List<StockExchange> members) {
        this.members = members;
    }

    /**
     * 이 universe에 속한 거래소 목록을 반환한다.
     */
    public List<StockExchange> members() {
        return members;
    }
}
