package com.dove.modelserving.domain.enums;

/**
 * 모델의 채점 활성 상태.
 */
public enum ModelStatus {
    /** 일일 채점 대상. */
    ACTIVE,
    /** 채점 제외(피처 불일치·수동 비활성 등). */
    INACTIVE;

    /**
     * 이름과 일치하는 값을 반환한다. 일치하지 않으면 null.
     */
    public static ModelStatus parseOrNull(String name) {
        if (name == null) return null;
        for (ModelStatus status : values()) {
            if (status.name().equals(name)) return status;
        }
        return null;
    }
}
