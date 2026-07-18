package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.enums.PortfolioSharePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 계좌 공유 요청.
 *
 * @param accountId       공유할 계좌 ID
 * @param granteeUsername 공유받을 회원의 아이디(username)
 * @param permission      부여 권한
 */
public record GrantPortfolioShareRequest(
        @NotNull Long accountId,
        @NotBlank String granteeUsername,
        @NotNull PortfolioSharePermission permission
) {}
