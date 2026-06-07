package com.dove.api.member.featuregrant.dto;

import com.dove.api.member.featuregrant.enums.GrantAction;
import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 기능 권한 변경 요청.
 *
 * @param featureCode 기능 코드
 * @param action      부여/회수 동작
 */
public record UpdateUserFeatureRequest(
        @NotNull FeatureCode featureCode,
        @NotNull GrantAction action
) {
}
