package com.dove.api.search.stocktag.dto;

import java.util.List;

/**
 * 종목 분류 메타 + 값 목록 전체.
 */
public record StockTagsResponse(
        List<StockTagFieldGroup> tagFields,
        List<StockTagNumericField> numericFields
) {
}
