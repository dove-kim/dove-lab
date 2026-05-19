package com.dove.api.auth.dto;

public record RegisterResponse(
        String accessToken,
        String username,
        String name,
        String role
) {}
