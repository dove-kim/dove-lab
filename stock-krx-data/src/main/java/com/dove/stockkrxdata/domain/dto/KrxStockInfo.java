package com.dove.stockkrxdata.domain.dto;

import com.dove.stockkrxdata.domain.enums.MarketType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KrxStockInfo(
        LocalDate tradingDate,
        MarketType marketType,
        String stockName,
        String stockCode,
        Long tradingVolume,
        BigDecimal openingPrice,
        BigDecimal closingPrice,
        BigDecimal lowestPrice,
        BigDecimal highestPrice
) {
}
