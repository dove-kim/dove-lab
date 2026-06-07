package com.dove.api.member.admin.dto;

import com.dove.auth.domain.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * 회원 권한 변경 요청.
 *
 * @param role 변경할 권한 등급
 */
public record ChangeRoleRequest(@NotNull MemberRole role) {}
