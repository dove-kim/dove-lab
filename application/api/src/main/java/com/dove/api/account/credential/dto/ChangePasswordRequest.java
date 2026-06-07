package com.dove.api.account.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청.
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword     새 비밀번호
 */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 4, max = 50) String newPassword
) {
}
