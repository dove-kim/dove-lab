package com.dove.api.account.credential.controller;

import com.dove.api.account.credential.dto.ChangePasswordRequest;
import com.dove.api.account.credential.dto.PasswordChangeResponse;
import com.dove.api.account.credential.service.PasswordService;
import com.dove.api.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본인 비밀번호 변경 API.
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * 현재 비밀번호를 검증하고 새 비밀번호로 변경한 뒤 갱신된 토큰을 반환한다.
     */
    @PatchMapping("/password")
    public PasswordChangeResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid ChangePasswordRequest request) {
        String newToken = passwordService.changePassword(user, request.currentPassword(), request.newPassword());
        return new PasswordChangeResponse(newToken);
    }
}
