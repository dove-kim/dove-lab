package com.dove.api.portfolio.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * 보유 종목 연 배당수익률 설정 요청.
 *
 * @param annualDividendPct 연 배당수익률(%). null이면 해제.
 */
public record SetHoldingDividendRequest(
        @PositiveOrZero Double annualDividendPct
) {}
