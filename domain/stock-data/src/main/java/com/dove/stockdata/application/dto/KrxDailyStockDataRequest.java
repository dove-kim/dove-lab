package com.dove.stockdata.application.dto;

import com.dove.stockdata.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * KRX_STOCK_PRICE_QUERY 토픽의 일별 주가 조회 요청 DTO.
 * eventVersion으로 메시지 스키마 버전을 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KrxDailyStockDataRequest {
    private int eventVersion = 3;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;

    private MarketType marketType;

    public KrxDailyStockDataRequest(LocalDate baseDate, MarketType marketType) {
        this.baseDate = baseDate;
        this.marketType = marketType;
    }

    public KrxDailyStockDataRequest(int eventVersion, LocalDate baseDate, MarketType marketType) {
        this.eventVersion = eventVersion;
        this.baseDate = baseDate;
        this.marketType = marketType;
    }
}
