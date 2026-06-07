package com.dove.api.member.invite.dto;

import com.dove.auth.domain.entity.InviteCode;

import java.time.LocalDateTime;

/**
 * 초대 코드 응답.
 *
 * @param id        초대 코드 ID
 * @param code      초대 코드 문자열
 * @param role      부여 권한 등급
 * @param expiresAt 만료 일시
 * @param usedAt    사용 일시
 * @param createdBy 생성자
 * @param createdAt 생성 일시
 */
public record InviteCodeResponse(
        Long id,
        String code,
        String role,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        String createdBy,
        LocalDateTime createdAt
) {
    public static InviteCodeResponse from(InviteCode c) {
        return new InviteCodeResponse(
                c.getId(), c.getCode(), c.getRole().name(),
                c.getExpiresAt(), c.getUsedAt(), c.getCreatedBy(), c.getCreatedAt());
    }
}
