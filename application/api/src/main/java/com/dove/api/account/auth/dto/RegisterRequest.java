package com.dove.api.account.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 가입 요청.
 *
 * @param inviteCode 초대 코드
 * @param username   아이디 (영문 소문자·숫자)
 * @param password   비밀번호
 * @param email      이메일
 * @param name       표시 이름
 */
public record RegisterRequest(
        @NotBlank String inviteCode,
        @NotBlank @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영어 소문자와 숫자만 사용 가능합니다") @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 4, max = 100) String password,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 50) String name
) {
}
