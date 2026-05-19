package com.dove.api.stock.listing.dto;

import com.dove.stock.domain.entity.Stock;

import java.time.LocalDate;

public record StockResponse(
        String code,
        String isinCode,
        String name,
        String marketType,
        LocalDate listingDate
) {
    public static StockResponse from(Stock s) {
        return new StockResponse(
                s.getId().getCode(),
                s.getIsinCode(),
                s.getName(),
                s.getId().getMarketType().name(),
                s.getListingDate()
        );
    }
}
