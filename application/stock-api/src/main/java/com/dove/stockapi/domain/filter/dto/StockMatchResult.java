package com.dove.stockapi.domain.filter.dto;

public record StockMatchResult(
        String code,
        String name,
        String marketType,
        Long closePrice,
        Long volume
) {}
