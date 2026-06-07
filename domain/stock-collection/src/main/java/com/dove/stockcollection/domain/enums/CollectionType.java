package com.dove.stockcollection.domain.enums;

/**
 * 수집 작업 유형.
 */
public enum CollectionType {
    STOCK,    // 종목 목록 수집 (KRX)
    PRICE,    // 주가 수집 (KIS)
    EVENT,    // 권리 이벤트 수집 (KIS 예탁원정보)
    INVESTOR  // 투자자매매동향 수집 (KIS)
}
