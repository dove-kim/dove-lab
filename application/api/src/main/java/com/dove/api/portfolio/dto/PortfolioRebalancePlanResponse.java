package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioRebalancePlan;
import com.dove.portfolio.domain.value.RebalancePlanConfig;

/**
 * 리밸런싱 계획 응답.
 *
 * @param id     계획 ID
 * @param name   계획명
 * @param config 계획 설정(슬롯 수·참여율·종목 배분·전략 현금)
 */
public record PortfolioRebalancePlanResponse(Long id, String name, RebalancePlanConfig config) {
    public static PortfolioRebalancePlanResponse of(PortfolioRebalancePlan p) {
        return new PortfolioRebalancePlanResponse(p.getId(), p.getName(), p.getConfig());
    }
}
