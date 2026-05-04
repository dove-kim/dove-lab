package com.dove.stockapi.domain.admin.dto;

import com.dove.member.domain.entity.InviteCode;

import java.time.LocalDateTime;

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
