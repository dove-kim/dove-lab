package com.dove.api.account.password.controller;

import com.dove.api.account.password.dto.ChangePasswordRequest;
import com.dove.api.account.password.dto.PasswordChangeResponse;
import com.dove.api.account.password.service.PasswordService;
import com.dove.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    @PatchMapping("/password")
    public PasswordChangeResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid ChangePasswordRequest request) {
        String newToken = passwordService.changePassword(user, request.currentPassword(), request.newPassword());
        return new PasswordChangeResponse(newToken);
    }
}
