package com.dove.api.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 계좌 수정 요청.
 *
 * @param name        계좌명
 * @param brokerName  증권사명
 * @param description 설명
 */
public record UpdatePortfolioAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String brokerName,
        @Size(max = 500) String description
) {}
