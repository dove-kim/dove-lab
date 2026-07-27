package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.value.RebalancePlanConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 리밸런싱 계획 저장 요청 — 같은 이름이면 갱신.
 *
 * @param name   계획명
 * @param config 계획 설정(슬롯 수·참여율·종목 배분·전략 현금)
 */
public record SavePortfolioRebalancePlanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull RebalancePlanConfig config
) {}
