package com.dove.stockbatch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KrxDailyStockDataRequest {
    /**
     * 이벤트 스키마 버전
     */
    private final int eventVersion = 2;

    /**
     * 요청 날짜
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate baseDate;

    public KrxDailyStockDataRequest(LocalDate baseDate) {
        this.baseDate = baseDate;
    }
}
