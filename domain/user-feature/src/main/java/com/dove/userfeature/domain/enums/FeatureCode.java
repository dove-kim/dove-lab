package com.dove.userfeature.domain.enums;

/**
 * 사용자에게 부여/회수되는 기능 코드.
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
