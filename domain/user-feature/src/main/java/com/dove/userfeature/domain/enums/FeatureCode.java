package com.dove.userfeature.domain.enums;

/**
 * 기능 코드.
 *
 * <p>각 기능은 하나의 모듈에 속하며, 사용자에게 개별 부여/회수된다.
 * 모듈 정보는 DB에 저장하지 않고 이 enum에서 파생한다.
 */
public enum FeatureCode {
    STOCK_SEARCH(ModuleCode.STOCK, "주식 종목 검색"),
    STOCK_LEDGER(ModuleCode.STOCK, "주식 장부"),
    BUDGET(ModuleCode.BUDGET, "가계부");

    private final ModuleCode module;
    private final String label;

    FeatureCode(ModuleCode module, String label) {
        this.module = module;
        this.label = label;
    }

    public ModuleCode getModule() {
        return module;
    }

    public String getLabel() {
        return label;
    }
}
