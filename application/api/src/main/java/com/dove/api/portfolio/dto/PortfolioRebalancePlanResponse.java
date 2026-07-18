package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioRebalancePlan;
import com.dove.portfolio.domain.value.RebalancePlanEntry;

import java.util.List;

/**
 * 리밸런싱 계획 응답.
 *
 * @param id      계획 ID
 * @param name    계획명
 * @param entries 목표 배분 항목
 */
public record PortfolioRebalancePlanResponse(Long id, String name, List<RebalancePlanEntry> entries) {
    public static PortfolioRebalancePlanResponse of(PortfolioRebalancePlan p) {
        return new PortfolioRebalancePlanResponse(p.getId(), p.getName(), p.getEntries());
    }
}
