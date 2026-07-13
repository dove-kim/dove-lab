package com.dove.api.member.modelgrant.dto;

import com.dove.api.member.capabilitygrant.enums.GrantAction;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 모델 점수 접근 변경 요청.
 *
 * @param modelId 모델 ID
 * @param action  부여/회수 동작
 */
public record UpdateUserModelRequest(
        @NotNull Long modelId,
        @NotNull GrantAction action
) {
}
