package com.dove.userfeature.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 기능에 속한 하위 메뉴 코드.
 */
public enum SubMenuCode {
    STOCK_SEARCH_MAIN(FeatureCode.STOCK_SEARCH),
    STOCK_SEARCH_FILTER(FeatureCode.STOCK_SEARCH),
    STOCK_FILTERS(FeatureCode.STOCK_SEARCH);

    private final FeatureCode feature;

    SubMenuCode(FeatureCode feature) {
        this.feature = feature;
    }

    public FeatureCode getFeature() {
        return feature;
    }

    /**
     * 특정 기능에 속한 하위 메뉴 코드 목록을 반환한다.
     */
    public static List<SubMenuCode> byFeature(FeatureCode featureCode) {
        return Arrays.stream(values())
                .filter(s -> s.feature == featureCode)
                .toList();
    }
}
