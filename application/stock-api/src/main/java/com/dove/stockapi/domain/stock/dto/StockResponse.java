package com.dove.stockapi.domain.stock.dto;

import com.dove.stock.domain.entity.Stock;

import java.time.LocalDate;

public record StockResponse(
        String code,
        String name,
        String marketType,
        String tradingStatus,
        LocalDate listingDate
) {
    public static StockResponse from(Stock s) {
        return new StockResponse(
                s.getId().getCode(),
                s.getName(),
                s.getId().getMarketType().name(),
                s.getTradingStatus().name(),
                s.getListingDate()
        );
    }
}
