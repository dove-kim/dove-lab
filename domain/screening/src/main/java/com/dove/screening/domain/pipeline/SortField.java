package com.dove.screening.domain.pipeline;

/**
 * 파이프라인 RANK 단계의 정렬 기준 필드.
 */
public enum SortField {
    CHANGE_RATE,
    MARKET_CAP,
    VOLUME;

    /**
     * 이름으로 정렬 필드를 찾되, 없으면 null을 반환한다.
     */
    public static SortField parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
