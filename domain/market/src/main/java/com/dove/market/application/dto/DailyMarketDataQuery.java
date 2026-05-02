package com.dove.market.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** DAILY_MARKET_DATA_QUERY 토픽의 시장 데이터 조회 요청. 종목·주가를 단일 이벤트로 통합. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyMarketDataQuery {
    public static final String TOPIC = "DAILY_MARKET_DATA_QUERY";

    private int eventVersion = 1;

    private MarketType marketType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;

    public DailyMarketDataQuery(MarketType marketType, LocalDate baseDate) {
        this.marketType = marketType;
        this.baseDate = baseDate;
    }

    public String partitionKey() {
        return marketType.name();
    }
}
