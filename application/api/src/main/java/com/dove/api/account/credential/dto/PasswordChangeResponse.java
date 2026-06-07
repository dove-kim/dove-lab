package com.dove.api.account.credential.dto;

/**
 * 비밀번호 변경 응답.
 *
 * @param accessToken 재발급된 액세스 토큰
 */
public record PasswordChangeResponse(String accessToken) {
}
