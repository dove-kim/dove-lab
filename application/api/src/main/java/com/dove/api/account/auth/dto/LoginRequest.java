package com.dove.api.account.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 로그인 요청.
 *
 * @param username   아이디 (영문 소문자·숫자)
 * @param password   비밀번호
 * @param rememberMe 로그인 상태 유지 여부
 */
public record LoginRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영어 소문자와 숫자만 사용 가능합니다")
        String username,
        @NotBlank String password,
        boolean rememberMe
) {
}
