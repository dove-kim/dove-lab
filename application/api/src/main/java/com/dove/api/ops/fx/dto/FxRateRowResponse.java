package com.dove.api.ops.fx.dto;

import com.dove.portfolio.domain.entity.PortfolioFxRate;

import java.math.BigDecimal;

/**
 * 고시 환율 한 건의 응답.
 *
 * @param rateDate 고시일(ISO)
 * @param currency 원통화 코드
 * @param rate     원통화 1단위당 원화
 */
public record FxRateRowResponse(String rateDate, String currency, BigDecimal rate) {
    /**
     * 환율 이력을 응답으로 변환한다.
     */
    public static FxRateRowResponse from(PortfolioFxRate r) {
        return new FxRateRowResponse(r.getRateDate().toString(), r.getCurrency(), r.getRate());
    }
}
