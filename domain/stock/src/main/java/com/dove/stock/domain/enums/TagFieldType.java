package com.dove.stock.domain.enums;

/**
 * 분류 차원의 값 성격.
 * CATEGORY: 한정된 분류값 집합(예: 주권/ETF, 반도체/화학). IN·제외로 필터.
 * BOOLEAN: Y/N 플래그(예: KOSPI200 편입 여부).
 */
public enum TagFieldType {
    CATEGORY,
    BOOLEAN
}
