package com.dove.api.search.stocktag.dto;

import com.dove.stock.domain.entity.StockTagValue;

/**
 * 분류 값 하나 (id·원문·표시명).
 */
public record StockTagValueItem(
        Long id,
        String value,
        String label
) {
    public static StockTagValueItem from(StockTagValue e) {
        return new StockTagValueItem(e.getId(), e.getValue(), e.getLabel());
    }
}
