package com.dove.market.domain.enums;

import java.util.List;

/**
 * 한국 주식 시장 유형.
 */
public enum MarketType {
    KOSPI, KOSDAQ, KONEX;

    /**
     * KRX가 운영하는 전체 시장.
     */
    public static final List<MarketType> KRX_MARKETS = List.of(KOSPI, KOSDAQ, KONEX);
}
