package com.dove.stock.domain.enums;

/**
 * 종목 분류(태그) 차원. 값 목록은 동적이지만 차원 자체는 한정 집합이라 enum으로 고정한다.
 */
public enum TagField {

    SECUGRP(TagSource.KRX, TagFieldType.CATEGORY, "증권그룹"),
    STOCK_TYPE(TagSource.KRX, TagFieldType.CATEGORY, "주권종류"),

    INDUSTRY_LCLS(TagSource.KIS, TagFieldType.CATEGORY, "업종 대분류"),
    INDUSTRY_MCLS(TagSource.KIS, TagFieldType.CATEGORY, "업종 중분류"),
    INDUSTRY_SCLS(TagSource.KIS, TagFieldType.CATEGORY, "업종 소분류"),
    STD_INDUSTRY(TagSource.KIS, TagFieldType.CATEGORY, "표준산업분류"),
    PRDT_CLSF(TagSource.KIS, TagFieldType.CATEGORY, "상품분류"),

    KOSPI200(TagSource.KIS, TagFieldType.BOOLEAN, "KOSPI200"),
    TR_STOP(TagSource.KIS, TagFieldType.BOOLEAN, "거래정지"),
    ADMIN_ITEM(TagSource.KIS, TagFieldType.BOOLEAN, "관리종목");

    private final TagSource source;
    private final TagFieldType type;
    private final String label;

    TagField(TagSource source, TagFieldType type, String label) {
        this.source = source;
        this.type = type;
        this.label = label;
    }

    public TagSource source() { return source; }
    public TagFieldType type() { return type; }
    public String label() { return label; }

    public boolean isCategory() { return type == TagFieldType.CATEGORY; }
}
