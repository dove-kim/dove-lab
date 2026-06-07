package com.dove.api.account.auth.dto;

/**
 * 회원 가입 응답.
 *
 * @param accessToken  액세스 토큰
 * @param refreshToken 리프레시 토큰
 * @param username     아이디
 * @param name         표시 이름
 * @param role         권한 등급
 */
public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String username,
        String name,
        String role
) {
}
