package com.dove.api.account.auth.dto;

/**
 * 로그인 응답.
 *
 * @param accessToken  액세스 토큰
 * @param refreshToken 리프레시 토큰
 * @param username     아이디
 * @param name         표시 이름
 * @param role         권한
 * @param rememberMe   로그인 상태 유지 여부
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String username,
        String name,
        String role,
        boolean rememberMe
) {}
