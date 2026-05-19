package com.dove.userfeature.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 하위 메뉴 코드.
 *
 * <p>각 하위 메뉴는 하나의 기능(FeatureCode)에 속한다.
 * 기능 부여 시 소속 하위 메뉴가 자동으로 함께 부여된다.
 */
public enum SubMenuCode {
    STOCK_SEARCH_MAIN(FeatureCode.STOCK_SEARCH),
    STOCK_SEARCH_FILTER(FeatureCode.STOCK_SEARCH),
    STOCK_SEARCH_SETS(FeatureCode.STOCK_SEARCH);

    private final FeatureCode feature;

    SubMenuCode(FeatureCode feature) {
        this.feature = feature;
    }

    public FeatureCode getFeature() {
        return feature;
    }

    public static List<SubMenuCode> byFeature(FeatureCode featureCode) {
        return Arrays.stream(values())
                .filter(s -> s.feature == featureCode)
                .toList();
    }
}
