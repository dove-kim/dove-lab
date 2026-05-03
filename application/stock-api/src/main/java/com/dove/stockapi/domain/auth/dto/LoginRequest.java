package com.dove.stockapi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영어 소문자와 숫자만 사용 가능합니다")
        String username,
        @NotBlank String password,
        boolean rememberMe
) {}
