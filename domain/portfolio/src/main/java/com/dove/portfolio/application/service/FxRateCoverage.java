package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioFxRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 한 통화의 환율 이력 보유 현황.
 *
 * @param currency     원통화 코드
 * @param earliestDate 가장 이른 고시일(이력 없으면 null)
 * @param latestDate   가장 최근 고시일(이력 없으면 null)
 * @param latestRate   가장 최근 환율(이력 없으면 null)
 * @param rowCount     보유 행수
 */
public record FxRateCoverage(
        String currency,
        LocalDate earliestDate,
        LocalDate latestDate,
        BigDecimal latestRate,
        int rowCount
) {
    /**
     * 통화의 환율 이력으로 현황을 집계한다.
     */
    public static FxRateCoverage of(String currency, List<PortfolioFxRate> rates) {
        if (rates.isEmpty()) {
            return new FxRateCoverage(currency, null, null, null, 0);
        }
        PortfolioFxRate earliest = rates.stream().min(Comparator.comparing(PortfolioFxRate::getRateDate)).orElseThrow();
        PortfolioFxRate latest = rates.stream().max(Comparator.comparing(PortfolioFxRate::getRateDate)).orElseThrow();
        return new FxRateCoverage(currency, earliest.getRateDate(), latest.getRateDate(), latest.getRate(),
                rates.size());
    }
}
