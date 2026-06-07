package com.dove.api.member.featuregrant.controller;

import com.dove.api.member.featuregrant.dto.UpdateUserFeatureRequest;
import com.dove.api.member.featuregrant.dto.UpdateUserSubMenuRequest;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.userfeature.application.service.MemberFeatureGrantCommandService;
import com.dove.userfeature.application.dto.MemberMenuView;
import com.dove.userfeature.application.service.MemberMenuDisplayQueryService;
import com.dove.userfeature.application.service.MemberSubMenuGrantCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 기능·하위메뉴 부여 관리 API.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class MemberFeatureGrantController {

    private final MemberFeatureGrantCommandService grantCommandService;
    private final MemberSubMenuGrantCommandService subMenuGrantCommandService;
    private final MemberMenuDisplayQueryService menuQueryService;

    /**
     * 회원에게 기능 권한을 부여하거나 회수한다.
     */
    @PatchMapping("/{userId}/features")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFeature(
            @PathVariable("userId") Long memberId,
            @RequestBody @Valid UpdateUserFeatureRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> grantCommandService.grant(memberId, request.featureCode(), admin.memberId());
            case REVOKE -> grantCommandService.revoke(memberId, request.featureCode());
        }
    }

    /**
     * 회원에게 서브메뉴 권한을 부여하거나 회수한다.
     */
    @PatchMapping("/{userId}/sub-menus")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSubMenu(
            @PathVariable("userId") Long memberId,
            @RequestBody @Valid UpdateUserSubMenuRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> subMenuGrantCommandService.grant(memberId, request.subMenuCode(), admin.memberId());
            case REVOKE -> subMenuGrantCommandService.revoke(memberId, request.subMenuCode());
        }
    }

    /**
     * 특정 회원의 메뉴 구성을 조회한다.
     */
    @GetMapping("/{userId}/menu")
    public MemberMenuView getMemberMenu(@PathVariable("userId") Long memberId) {
        return menuQueryService.buildMenuForMember(memberId);
    }
}
