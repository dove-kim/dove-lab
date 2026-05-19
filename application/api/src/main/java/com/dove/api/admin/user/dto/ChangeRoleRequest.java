package com.dove.api.admin.user.dto;

import com.dove.user.domain.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull MemberRole role) {}
