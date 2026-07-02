package com.dove.screening.domain.value;

/**
 * 종목 상태 — 거래정지·관리종목.
 */
public enum StockStatusType {
    TRADING_HALT,
    ADMIN_ITEM;

    /**
     * 이름으로 상태 종류를 찾되, 없으면 null을 반환한다.
     */
    public static StockStatusType parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
