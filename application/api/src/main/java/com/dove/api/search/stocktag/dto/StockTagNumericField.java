package com.dove.api.search.stocktag.dto;

/**
 * 수치형 필드 메타 (범위 조건 UI용).
 */
public record StockTagNumericField(
        String field,
        String label,
        String source
) {
}
