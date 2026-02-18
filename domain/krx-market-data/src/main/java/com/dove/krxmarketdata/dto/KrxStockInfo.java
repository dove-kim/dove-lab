package com.dove.krxmarketdata.dto;

import com.dove.stockdata.enums.MarketType;

import java.time.LocalDate;

public record KrxStockInfo(
        LocalDate tradingDate,
        MarketType marketType,
        String stockName,
        String stockCode,
        Long tradingVolume,
        Long openingPrice,
        Long closingPrice,
        Long lowestPrice,
        Long highestPrice
) {
}
