package com.dove.api.search.stocktag.dto;

import java.util.List;

/**
 * 분류 차원(field) 하나와 그 값 목록.
 *
 * @param field 분류 차원 코드
 * @param label 표시 라벨
 * @param source 출처
 * @param type 차원 유형(CATEGORY/BOOLEAN)
 * @param values 값 목록(CATEGORY만 채워짐)
 */
public record StockTagFieldGroup(
        String field,
        String label,
        String source,
        String type,
        List<StockTagValueItem> values
) {
}
