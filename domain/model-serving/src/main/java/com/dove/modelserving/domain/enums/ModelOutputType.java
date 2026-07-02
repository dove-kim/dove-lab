package com.dove.modelserving.domain.enums;

/**
 * 모델 출력값의 의미 구분.
 */
public enum ModelOutputType {
    /** 0~1 확률(보정된 이익마감 확률 등). */
    PROBABILITY,
    /** 연속 회귀값. */
    REGRESSION;

    /**
     * 이름과 일치하는 값을 반환한다. 일치하지 않으면 null.
     */
    public static ModelOutputType parseOrNull(String name) {
        if (name == null) return null;
        for (ModelOutputType type : values()) {
            if (type.name().equals(name)) return type;
        }
        return null;
    }
}
