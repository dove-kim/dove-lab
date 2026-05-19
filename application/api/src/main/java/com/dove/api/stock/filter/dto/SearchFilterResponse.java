package com.dove.api.stock.filter.dto;

import com.dove.screening.domain.entity.SearchFilter;

import java.time.LocalDateTime;
import java.util.List;

public record SearchFilterResponse(
        Long id,
        String name,
        String dateRule,
        List<String> markets,
        String expression,
        Long includeStockSetId,
        Long excludeStockSetId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SearchFilterResponse from(SearchFilter f) {
        return new SearchFilterResponse(
                f.getId(), f.getName(),
                f.getDateRule().name(),
                f.getMarketList().stream().map(Enum::name).toList(),
                f.getExpression(),
                f.getIncludeStockSetId(),
                f.getExcludeStockSetId(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }
}
