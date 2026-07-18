package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioAccount;

import java.time.LocalDateTime;

/**
 * 포트폴리오 계좌 응답.
 *
 * @param id          계좌 ID
 * @param name        계좌명
 * @param brokerName  증권사명
 * @param description 설명
 * @param createdAt   생성 일시
 */
public record PortfolioAccountResponse(
        Long id,
        String name,
        String brokerName,
        String description,
        LocalDateTime createdAt
) {
    public static PortfolioAccountResponse from(PortfolioAccount a) {
        return new PortfolioAccountResponse(a.getId(), a.getName(), a.getBrokerName(), a.getDescription(), a.getCreatedAt());
    }
}
