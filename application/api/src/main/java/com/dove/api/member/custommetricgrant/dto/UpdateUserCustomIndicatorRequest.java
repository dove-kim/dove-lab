package com.dove.api.member.custommetricgrant.dto;

import com.dove.api.member.capabilitygrant.enums.GrantAction;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 커스텀 지표 접근 변경 요청.
 *
 * @param metricId 커스텀 지표 ID
 * @param action   부여/회수 동작
 */
public record UpdateUserCustomIndicatorRequest(
        @NotNull Long metricId,
        @NotNull GrantAction action
) {
}
