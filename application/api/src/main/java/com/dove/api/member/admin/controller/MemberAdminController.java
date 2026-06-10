package com.dove.api.member.admin.controller;

import com.dove.api.account.credential.dto.ResetPasswordResponse;
import com.dove.api.account.credential.service.AdminPasswordResetService;
import com.dove.api.member.admin.dto.ChangeRoleRequest;
import com.dove.api.member.admin.dto.MemberSummaryResponse;
import com.dove.api.member.admin.service.MemberSummaryQueryService;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.user.application.service.MemberProfileCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 회원 신원·목록 관리 API.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class MemberAdminController {

    private final MemberSummaryQueryService memberSummaryQueryService;
    private final MemberProfileCommandService memberProfileCommandService;
    private final AdminPasswordResetService adminPasswordResetService;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 전체 회원 요약 목록을 반환한다.
     */
    @GetMapping
    public List<MemberSummaryResponse> listMembers() {
        return memberSummaryQueryService.findAllSummaries().stream()
                .map(MemberSummaryResponse::from)
                .toList();
    }

    /**
     * ROOT 전용 — 역할 변경.
     */
    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole(Role.ROOT)
    public void changeRole(@PathVariable Long userId,
                           @RequestBody @Valid ChangeRoleRequest request) {
        try {
            memberProfileCommandService.changeRole(userId, request.role());
            forcedLogoutService.markLogoutNow(userId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ROOT_ROLE_IMMUTABLE");
        }
    }

    /**
     * ROOT 전용 — 비밀번호 초기화.
     */
    @PostMapping("/{userId}/reset-password")
    @RequireRole(Role.ROOT)
    public ResetPasswordResponse resetPassword(@PathVariable Long userId) {
        return new ResetPasswordResponse(adminPasswordResetService.resetPassword(userId));
    }
}
