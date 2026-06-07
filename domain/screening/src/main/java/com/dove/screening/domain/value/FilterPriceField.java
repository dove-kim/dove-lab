package com.dove.screening.domain.value;

/**
 * 검색식이 참조하는 가격 필드.
 */
public enum FilterPriceField {
    OPEN,
    HIGH,
    LOW,
    CLOSE;

    /**
     * 이름으로 가격 필드를 찾되, 없으면 null을 반환한다.
     */
    public static FilterPriceField parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
