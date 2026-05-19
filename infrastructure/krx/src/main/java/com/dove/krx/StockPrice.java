package com.dove.krx;

/** 일별 주가 정보. marketType·date는 DailyMarketData에서 관리하므로 포함하지 않음. */
public record StockPrice(
        String stockCode,
        long tradingVolume,
        long openingPrice,
        long closingPrice,
        long lowestPrice,
        long highestPrice
) {}
