package com.dove.stockconsumer.dto;

import com.dove.stockdata.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class KrxDailyStockDataRequest {
    /**
     * 이벤트 스키마 버전
     */
    private int eventVersion;

    /**
     * 시장 타입
     */
    private MarketType marketType;

    /**
     * 요청 날짜
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;
}
