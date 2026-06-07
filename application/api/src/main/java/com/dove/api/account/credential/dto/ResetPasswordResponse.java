package com.dove.api.account.credential.dto;

/**
 * 비밀번호 초기화 응답.
 *
 * @param temporaryPassword 임시 비밀번호
 */
public record ResetPasswordResponse(String temporaryPassword) {
}
