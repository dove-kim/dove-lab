package com.dove.api.auth.dto;

import com.dove.user.domain.entity.MemberRole;

public record LoginResult(
        String accessToken,
        Long memberId,
        String username,
        String name,
        MemberRole role,
        boolean rememberMe,
        boolean mustChangePassword
) {}
