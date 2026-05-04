package com.dove.stockapi.domain.auth.dto;

public record RegisterResponse(
        String accessToken,
        String username,
        String name,
        String role
) {}
