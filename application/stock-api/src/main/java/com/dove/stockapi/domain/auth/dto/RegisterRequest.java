package com.dove.stockapi.domain.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String inviteCode,
        @NotBlank @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영어 소문자와 숫자만 사용 가능합니다") @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 4, max = 100) String password,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 50) String name
) {}
