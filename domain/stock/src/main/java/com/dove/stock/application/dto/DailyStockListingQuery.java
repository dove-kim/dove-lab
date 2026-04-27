package com.dove.stock.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** STOCK_LISTING_QUERY 토픽의 상장 종목 조회 요청. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStockListingQuery {
    public static final String TOPIC = "STOCK_LISTING_QUERY";

    private int eventVersion = 1;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;

    private MarketType marketType;

    public DailyStockListingQuery(LocalDate baseDate, MarketType marketType) {
        this.baseDate = baseDate;
        this.marketType = marketType;
    }
}
