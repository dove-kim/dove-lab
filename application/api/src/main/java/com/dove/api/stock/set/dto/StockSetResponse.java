package com.dove.api.stock.set.dto;

import com.dove.screening.domain.entity.StockSet;

import java.time.LocalDateTime;
import java.util.List;

public record StockSetResponse(
        Long id,
        String name,
        List<String> codes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StockSetResponse from(StockSet s) {
        return new StockSetResponse(s.getId(), s.getName(), s.getCodeList(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
