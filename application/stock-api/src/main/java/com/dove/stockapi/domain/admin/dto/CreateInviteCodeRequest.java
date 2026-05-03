package com.dove.stockapi.domain.admin.dto;

import com.dove.member.domain.entity.MemberRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInviteCodeRequest(
        @NotNull MemberRole role,
        @Min(1) @Max(365) int expireDays
) {}
