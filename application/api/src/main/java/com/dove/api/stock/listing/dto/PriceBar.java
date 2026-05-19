package com.dove.api.stock.listing.dto;

import com.dove.stock.domain.entity.DailyStockPrice;

import java.time.LocalDate;

public record PriceBar(String date, PriceBarStatus status, Long open, Long high, Long low, Long close, Long volume) {

    public static PriceBar of(LocalDate date, DailyStockPrice price) {
        if (price == null) {
            return new PriceBar(date.toString(), PriceBarStatus.DELISTED, null, null, null, null, null);
        }
        if (price.getOpenPrice() == null || price.getVolume() == null || price.getVolume() == 0L) {
            return new PriceBar(date.toString(), PriceBarStatus.HALTED,
                    null, null, null, price.getClosePrice(), null);
        }
        return new PriceBar(date.toString(), PriceBarStatus.TRADING,
                price.getOpenPrice(), price.getHighPrice(),
                price.getLowPrice(), price.getClosePrice(),
                price.getVolume());
    }
}
