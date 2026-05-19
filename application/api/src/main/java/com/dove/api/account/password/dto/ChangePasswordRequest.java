package com.dove.api.account.password.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 4, max = 50) String newPassword
) {}
