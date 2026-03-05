package com.dove.technicalindicator.application.dto;

import com.dove.stockdata.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 날짜 기반 기술적 지표 계산 요청 DTO. 특정 거래일의 전 종목 지표 계산에 사용된다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechnicalIndicatorCalcRequest {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate tradeDate;

    private MarketType marketType;
}
