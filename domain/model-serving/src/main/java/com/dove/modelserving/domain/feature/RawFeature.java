package com.dove.modelserving.domain.feature;

/**
 * 진입존이 상수 임계로 참조할 수 있는 원시 시세 컬럼(STOCK_FEATURE_DAILY).
 */
public enum RawFeature {
    VOLUME, TURNOVER;

    /**
     * 이름으로 RawFeature를 찾고, 알 수 없는 이름이면 null을 반환한다.
     */
    public static RawFeature parseOrNull(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
