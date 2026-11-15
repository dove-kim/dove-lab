package com.dove.api.ops.fx.dto;

import com.dove.portfolio.application.service.FxRateCoverage;

import java.math.BigDecimal;

/**
 * 한 통화의 환율 이력 보유 현황 응답.
 *
 * @param currency     원통화 코드
 * @param earliestDate 가장 이른 고시일(ISO, 이력 없으면 null)
 * @param latestDate   가장 최근 고시일(ISO, 이력 없으면 null)
 * @param latestRate   가장 최근 환율(이력 없으면 null)
 * @param rowCount     보유 행수
 */
public record FxRateCoverageResponse(
        String currency,
        String earliestDate,
        String latestDate,
        BigDecimal latestRate,
        int rowCount
) {
    /**
     * 도메인 현황을 응답으로 변환한다.
     */
    public static FxRateCoverageResponse from(FxRateCoverage c) {
        return new FxRateCoverageResponse(
                c.currency(),
                c.earliestDate() != null ? c.earliestDate().toString() : null,
                c.latestDate() != null ? c.latestDate().toString() : null,
                c.latestRate(),
                c.rowCount());
    }
}
