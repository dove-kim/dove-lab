package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.value.RebalancePlanEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 리밸런싱 계획 저장 요청 — 같은 이름이면 갱신.
 *
 * @param name    계획명
 * @param entries 목표 배분 항목
 */
public record SavePortfolioRebalancePlanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull List<RebalancePlanEntry> entries
) {}
