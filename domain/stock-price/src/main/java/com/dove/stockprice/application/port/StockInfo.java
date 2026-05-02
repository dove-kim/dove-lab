package com.dove.stockprice.application.port;

import com.dove.market.domain.enums.MarketType;

import java.time.LocalDate;

/** 소스 중립 일별 종목 시세 정보. */
public record StockInfo(
        LocalDate tradingDate,
        MarketType marketType,
        String stockCode,
        Long tradingVolume,
        Long openingPrice,
        Long closingPrice,
        Long lowestPrice,
        Long highestPrice
) {
}
