package com.dove.modelserving.domain.zone;

/**
 * 진입존 조건식에서 쓰는 비교 연산자.
 */
public enum ZoneOperator {
    GE(">="),
    LE("<="),
    GT(">"),
    LT("<"),
    EQ("==");

    private final String token;

    ZoneOperator(String token) {
        this.token = token;
    }

    /**
     * 연산자 토큰 문자열.
     */
    public String token() {
        return token;
    }

    /**
     * 좌변(actual)과 우변(threshold)에 이 연산자를 적용한 결과.
     */
    public boolean test(double actual, double threshold) {
        return switch (this) {
            case GE -> actual >= threshold;
            case LE -> actual <= threshold;
            case GT -> actual > threshold;
            case LT -> actual < threshold;
            case EQ -> actual == threshold;
        };
    }
}
