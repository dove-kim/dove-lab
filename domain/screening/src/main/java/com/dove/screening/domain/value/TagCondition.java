package com.dove.screening.domain.value;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 태그 조건 — SECUGRP_NM / KIND_STKCERT_TP_NM / MARKET 기반 포함·제외 조건.
 *
 * @param field 분류 필드 ("SECURITY_GROUP" 또는 "STOCK_TYPE")
 * @param value 분류 값 이름 (예: "ETF", "REIT", "PREFERRED")
 * @param mode  포함("INCLUDE") 또는 제외("EXCLUDE")
 */
public record TagCondition(
        String field,
        String value,
        String mode
) {
    public static TagCondition include(String field, String value) {
        return new TagCondition(field, value, "INCLUDE");
    }

    public static TagCondition exclude(String field, String value) {
        return new TagCondition(field, value, "EXCLUDE");
    }

    @JsonIgnore
    public boolean isInclude() {
        return "INCLUDE".equals(mode);
    }

    @JsonIgnore
    public boolean isExclude() {
        return "EXCLUDE".equals(mode);
    }
}
