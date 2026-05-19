package com.dove.api.admin.feature.controller;

import com.dove.api.admin.dto.UserSummaryResponse;
import com.dove.api.admin.feature.dto.UpdateUserFeatureRequest;
import com.dove.api.admin.feature.dto.UpdateUserSubMenuRequest;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.security.AuthenticatedUser;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.userfeature.application.service.UserFeatureGrantCommandService;
import com.dove.userfeature.application.service.UserMenuDisplayQueryService;
import com.dove.userfeature.application.service.UserMenuDisplayQueryService.UserMenuView;
import com.dove.userfeature.application.service.UserSubMenuGrantCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserFeatureAdminController {

    private final UserFeatureGrantCommandService grantCommandService;
    private final UserSubMenuGrantCommandService subMenuGrantCommandService;
    private final UserMenuDisplayQueryService menuQueryService;
    private final MemberProfileQueryService memberProfileQueryService;
    private final CredentialQueryService credentialQueryService;

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

    @PatchMapping("/{userId}/features")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFeature(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserFeatureRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> grantCommandService.grant(userId, request.featureCode(), admin.memberId());
            case REVOKE -> grantCommandService.revoke(userId, request.featureCode());
        }
    }

    @PatchMapping("/{userId}/sub-menus")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSubMenu(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserSubMenuRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> subMenuGrantCommandService.grant(userId, request.subMenuCode(), admin.memberId());
            case REVOKE -> subMenuGrantCommandService.revoke(userId, request.subMenuCode());
        }
    }

    @GetMapping("/{userId}/menu")
    public UserMenuView getUserMenu(@PathVariable Long userId) {
        return menuQueryService.buildMenuForUser(userId);
    }
}
