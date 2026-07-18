package com.dove.portfolio.domain.enums;

/**
 * 계좌 공유 권한 — 열람 범위와 쓰기 허용 여부.
 */
public enum PortfolioSharePermission {

    /** 전체 열람(금액 포함). */
    READ,

    /** 상대값만 열람(금액 숨김, 비중·수익률만). */
    READ_RELATIVE,

    /** 열람 + 거래 쓰기 허용. */
    WRITE;

    /**
     * 금액을 숨겨야 하는 권한인지 여부.
     */
    public boolean hidesAmounts() {
        return this == READ_RELATIVE;
    }

    /**
     * 쓰기가 허용되는 권한인지 여부.
     */
    public boolean allowsWrite() {
        return this == WRITE;
    }
}
