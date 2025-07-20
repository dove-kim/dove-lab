package com.dove.stockkrxdata.dto;

import com.dove.stockkrxdata.enums.KrxMarketType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KrxStockInfo(
        LocalDate tradingDate,
        KrxMarketType krxMarketType,
        String stockName,
        String stockCode,
        Long tradingVolume,
        BigDecimal openingPrice,
        BigDecimal closingPrice,
        BigDecimal lowestPrice,
        BigDecimal highestPrice
) {
}
