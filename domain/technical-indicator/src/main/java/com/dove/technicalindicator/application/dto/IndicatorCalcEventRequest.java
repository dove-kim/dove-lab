package com.dove.technicalindicator.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * TECHNICAL_INDICATOR_CALC 토픽의 기술적 지표 계산 요청 DTO.
 * 특정 종목의 지정 날짜부터 지표 재계산을 트리거한다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndicatorCalcEventRequest {
    private MarketType marketType;
    private String stockCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate tradeDate;
}
