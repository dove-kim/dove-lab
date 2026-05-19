package com.dove.api.stock.filter.dto;

public record StockMatchResult(
        String code,
        String name,
        String marketType,
        Long closePrice,
        Long volume
) {}
