package com.dove.screening.domain.value;

/**
 * 검색식 비교 연산자.
 */
public enum FilterOperator {
    GT,
    GTE,
    LT,
    LTE,
    EQ,
    NEQ;

    /**
     * 이름으로 연산자를 찾되, 없으면 null을 반환한다.
     */
    public static FilterOperator parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 두 값을 이 연산자로 비교한 결과를 반환한다.
     */
    public boolean compare(double left, double right) {
        return switch (this) {
            case GT -> left > right;
            case GTE -> left >= right;
            case LT -> left < right;
            case LTE -> left <= right;
            case EQ -> Double.compare(left, right) == 0;
            case NEQ -> Double.compare(left, right) != 0;
        };
    }
}
