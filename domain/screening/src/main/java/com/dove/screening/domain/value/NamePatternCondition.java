package com.dove.screening.domain.value;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 종목명 패턴 조건 — 종목명이 패턴과 매칭되는 종목을 포함·제외한다.
 *
 * @param pattern   종목명 패턴 문자열 (예: "리츠")
 * @param mode      포함("INCLUDE") 또는 제외("EXCLUDE")
 * @param matchType 매칭 방식: "CONTAINS"(포함), "STARTS_WITH"(시작), "ENDS_WITH"(끝). null이면 CONTAINS.
 */
public record NamePatternCondition(
        String pattern,
        String mode,
        String matchType
) {
    public NamePatternCondition {
        if (matchType == null || matchType.isBlank()) matchType = "CONTAINS";
    }

    public static NamePatternCondition include(String pattern) {
        return new NamePatternCondition(pattern, "INCLUDE", "CONTAINS");
    }

    public static NamePatternCondition exclude(String pattern) {
        return new NamePatternCondition(pattern, "EXCLUDE", "CONTAINS");
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
