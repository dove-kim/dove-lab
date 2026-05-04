package com.dove.stockapi.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String username,
        String name,
        String role,
        boolean rememberMe
) {}
