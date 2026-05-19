package com.dove.api.admin.user.controller;

import com.dove.api.admin.dto.UserSummaryResponse;
import com.dove.api.admin.user.dto.ChangeRoleRequest;
import com.dove.api.admin.user.dto.ResetPasswordResponse;
import com.dove.api.admin.user.service.AdminUserService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/root/users")
@RequiredArgsConstructor
public class SuperAdminUserController {

    private final MemberProfileQueryService memberProfileQueryService;
    private final MemberProfileCommandService memberProfileCommandService;
    private final CredentialQueryService credentialQueryService;
    private final AdminUserService adminUserService;

    @GetMapping
    public List<UserSummaryResponse> listUsers() {
        List<MemberProfile> profiles = memberProfileQueryService.findAll();
        Map<Long, String> usernameMap = credentialQueryService.findUsernamesByMemberIds(
                profiles.stream().map(MemberProfile::getId).toList());
        return profiles.stream()
                .map(p -> new UserSummaryResponse(
                        p.getId(), p.getName(), p.getEmail(),
                        usernameMap.getOrDefault(p.getId(), ""), p.getRole().name()))
                .toList();
    }

    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@PathVariable Long userId,
                           @RequestBody @Valid ChangeRoleRequest request) {
        try {
            memberProfileCommandService.changeRole(userId, request.role());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ROOT_ROLE_IMMUTABLE");
        }
    }

    @PostMapping("/{userId}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long userId) {
        return new ResetPasswordResponse(adminUserService.resetPassword(userId));
    }
}
