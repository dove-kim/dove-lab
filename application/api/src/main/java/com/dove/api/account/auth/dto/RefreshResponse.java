package com.dove.api.account.auth.dto;

/**
 * 토큰 재발급 응답.
 *
 * @param accessToken  액세스 토큰
 * @param refreshToken 리프레시 토큰
 */
public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
