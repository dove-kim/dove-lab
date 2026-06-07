package com.dove.api.member.invite.dto;

import com.dove.auth.domain.enums.MemberRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 초대 코드 생성 요청.
 *
 * @param role       부여할 권한 등급
 * @param expireDays 만료 일수
 */
public record CreateInviteCodeRequest(
        @NotNull MemberRole role,
        @Min(1) @Max(365) int expireDays
) {
}
