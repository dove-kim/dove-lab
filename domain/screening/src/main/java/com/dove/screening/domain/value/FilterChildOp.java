package com.dove.screening.domain.value;

/**
 * 그룹 내 자식 노드 결합 연산자.
 */
public enum FilterChildOp {
    AND,
    OR,
    AND_NOT,
    OR_NOT;

    /**
     * 이름으로 결합 연산자를 찾되, 없으면 기본값을 반환한다.
     */
    public static FilterChildOp parseOrDefault(String name, FilterChildOp fallback) {
        if (name == null) return fallback;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * 누적 결과와 자식 결과를 이 연산자로 결합한 값을 반환한다.
     */
    public boolean combine(boolean acc, boolean child) {
        return switch (this) {
            case AND -> acc && child;
            case OR -> acc || child;
            case AND_NOT -> acc && !child;
            case OR_NOT -> acc || !child;
        };
    }
}
