package com.dove.technicalindicator.domain.enums;

public enum DateState {
    /** DAILY_STOCK_PRICE 존재 — 정상 거래일 */
    ACTIVE,
    /** 가격 없음, STOCK_LISTED_DATE에 해당 종목 행 존재 — 상장 중이나 거래 없음 (정지 등) */
    SUSPENDED,
    /** 가격 없음, 해당 종목 미상장, 같은 (market, date) 다른 종목 listing 존재 — 상폐/미상장 */
    DELISTED,
    /** 가격 없음, 해당 종목 미상장, (market, date) listing 행 0건 — listing 미동기화 */
    LISTING_NOT_SYNCED
}
