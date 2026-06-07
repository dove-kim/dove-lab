package com.dove.userfeature.domain.enums;

/**
 * 기능의 논리적 그룹을 나타내는 메뉴 모듈 코드.
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
