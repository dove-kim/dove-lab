package com.dove.api.member.capabilitygrant.dto;

import com.dove.api.member.capabilitygrant.enums.GrantAction;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 capability 권한 변경 요청.
 *
 * @param capability 권한 코드
 * @param action     부여/회수 동작
 */
public record UpdateUserCapabilityRequest(
        @NotNull Capability capability,
        @NotNull GrantAction action
) {
}
