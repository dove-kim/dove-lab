package com.dove.api.account.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청.
 *
 * @param refreshToken 리프레시 토큰
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
