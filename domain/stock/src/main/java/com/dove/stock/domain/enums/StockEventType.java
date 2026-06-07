package com.dove.stock.domain.enums;

/**
 * 종목 권리 이벤트 유형 (KIS 예탁원정보 기반).
 */
public enum StockEventType {
    DIVIDEND("배당"),
    RIGHTS_ISSUE("유상증자"),
    BONUS_ISSUE("무상증자"),
    MERGER_SPLIT("합병/분할"),
    PAR_CHANGE("액면교체"),
    CAP_REDUCTION("감자");

    private final String label;

    StockEventType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
