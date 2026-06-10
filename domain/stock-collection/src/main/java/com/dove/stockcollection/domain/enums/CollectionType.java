package com.dove.stockcollection.domain.enums;

/**
 * 수집 작업 유형.
 */
public enum CollectionType {
    /** @deprecated KRX+KIS 통합 타입. 기존 레코드 역호환용 — 신규는 STOCK_SYNC/STOCK_DETAIL 사용. */
    @Deprecated
    STOCK,
    STOCK_SYNC,   // 종목 목록 동기화 (KRX)
    STOCK_DETAIL, // 종목 상세 upsert (KIS)
    PRICE,        // 주가 수집 (KIS)
    EVENT,        // 권리 이벤트 수집 (KIS 예탁원정보)
    INVESTOR      // 투자자매매동향 수집 (KIS)
}
