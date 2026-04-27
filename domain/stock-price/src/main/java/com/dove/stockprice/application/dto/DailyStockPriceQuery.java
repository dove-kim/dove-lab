package com.dove.stockprice.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** STOCK_PRICE_QUERY 토픽의 일별 주가 조회 요청. 소스 중립. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStockPriceQuery {
    public static final String TOPIC = "STOCK_PRICE_QUERY";

    private int eventVersion = 1;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;

    private MarketType marketType;

    public DailyStockPriceQuery(LocalDate baseDate, MarketType marketType) {
        this.baseDate = baseDate;
        this.marketType = marketType;
    }
}
