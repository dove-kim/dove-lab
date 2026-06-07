package com.dove.stock.domain.enums;

/**
 * 수치형 종목 속성. 범위(이상/이하) 조건으로 필터한다.
 */
public enum NumericField {

    CAPITAL_AMOUNT(TagSource.KIS, "상장자본금"),
    FACE_VALUE(TagSource.KIS, "액면가"),
    LISTED_SHARES(TagSource.KIS, "상장주식수");

    private final TagSource source;
    private final String label;

    NumericField(TagSource source, String label) {
        this.source = source;
        this.label = label;
    }

    public TagSource source() { return source; }
    public String label() { return label; }
}
