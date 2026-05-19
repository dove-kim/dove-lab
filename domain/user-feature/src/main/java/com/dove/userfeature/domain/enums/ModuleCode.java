package com.dove.userfeature.domain.enums;

/**
 * 메뉴 모듈 코드.
 *
 * <p>모듈은 기능(FeatureCode)의 논리적 그룹이다.
 * 사용자는 모듈 단위로 순서를 조정하거나 숨길 수 있다.
 */
public enum ModuleCode {
    STOCK("주식"),
    BUDGET("가계부");

    private final String label;

    ModuleCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
