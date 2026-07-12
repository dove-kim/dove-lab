package com.dove.screening.domain.pipeline;

/**
 * 파이프라인 RANK 단계의 정렬 방향.
 */
public enum SortDirection {
    ASC,
    DESC;

    /**
     * 이름으로 정렬 방향을 찾되, 없으면 null을 반환한다.
     */
    public static SortDirection parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
