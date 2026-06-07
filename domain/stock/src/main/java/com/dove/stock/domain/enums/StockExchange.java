package com.dove.stock.domain.enums;

import com.dove.market.domain.enums.MarketType;

import java.util.List;

/**
 * 주가 데이터의 거래소/시장 구분.
 */
public enum StockExchange {
    KOSPI,      // KIS: J (KOSPI 종목)
    KOSDAQ,     // KIS: J (KOSDAQ 종목)
    KONEX,      // KIS: J (KONEX 종목)
    NXT,        // KIS: NX
    INTEGRATED; // KIS: UN (KRX+NXT 통합)

    public static StockExchange fromMarket(MarketType market) {
        return switch (market) {
            case KOSPI -> KOSPI;
            case KOSDAQ -> KOSDAQ;
            case KONEX -> KONEX;
        };
    }

    /** 이 거래소에서 조회 가능한 KRX 시장 목록. NXT·INTEGRATED는 KONEX 미취급. */
    public List<MarketType> toMarkets() {
        return switch (this) {
            case KOSPI -> List.of(MarketType.KOSPI);
            case KOSDAQ -> List.of(MarketType.KOSDAQ);
            case KONEX -> List.of(MarketType.KONEX);
            case NXT, INTEGRATED -> List.of(MarketType.KOSPI, MarketType.KOSDAQ);
        };
    }
}
