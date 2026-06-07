package com.dove.kis;

import com.dove.stock.domain.enums.StockExchange;

/**
 * KIS API FID_COND_MRKT_DIV_CODE 값.
 * J = KRX(KOSPI·KOSDAQ·KONEX), NX = NXT, UN = 통합.
 */
public enum KisMarketCode {
    J, NX, UN;

    /**
     * 거래소 enum을 KIS 시장 분류 코드로 변환한다.
     */
    public static KisMarketCode of(StockExchange exchange) {
        return switch (exchange) {
            case KOSPI, KOSDAQ, KONEX -> J;
            case NXT -> NX;
            case INTEGRATED -> UN;
        };
    }
}
