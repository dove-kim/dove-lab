package com.dove.krxmarketdata.application.dto;

import com.dove.stockdata.domain.enums.MarketType;

import java.time.LocalDate;

/**
 * KRX API 응답에서 변환된 종목별 일별 주가 정보.
 */
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
