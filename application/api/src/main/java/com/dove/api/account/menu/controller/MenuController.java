package com.dove.api.account.menu.controller;

import com.dove.api.account.menu.dto.FeatureReorderRequest;
import com.dove.api.account.menu.dto.ModuleReorderRequest;
import com.dove.api.account.menu.dto.SetHiddenRequest;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.userfeature.application.service.MemberMenuDisplayCommandService;
import com.dove.userfeature.application.dto.MemberMenuView;
import com.dove.userfeature.application.service.MemberMenuDisplayQueryService;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * 회원 개인의 메뉴 표시·순서·숨김 설정 API.
 */
@RestController
@RequestMapping("/account/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MemberMenuDisplayQueryService menuQueryService;
    private final MemberMenuDisplayCommandService menuCommandService;

    /**
     * 내 메뉴 조회
     */
    @GetMapping
    public MemberMenuView getMenu(@AuthenticationPrincipal AuthenticatedUser user) {
        return menuQueryService.buildMenuForMember(user.memberId());
    }

    /**
     * 모듈 순서 변경
     */
    @PatchMapping("/modules/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderModules(
            @RequestBody @Valid ModuleReorderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        menuCommandService.reorderModules(user.memberId(), request.modules());
    }

    /**
     * 모듈 내 기능 순서 변경
     */
    @PatchMapping("/modules/{module}/features/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderFeatures(
            @PathVariable ModuleCode module,
            @RequestBody @Valid FeatureReorderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        menuCommandService.reorderFeatures(user.memberId(), module, request.features());
    }

    /**
     * 기능 숨김 상태 변경
     */
    @PatchMapping("/features/{feature}/hidden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFeatureHidden(
            @PathVariable FeatureCode feature,
            @RequestBody SetHiddenRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            menuCommandService.setFeatureHidden(user.memberId(), feature, request.hidden());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FEATURE_DISPLAY_NOT_FOUND");
        }
    }

    /**
     * 모듈 숨김 상태 변경
     */
    @PatchMapping("/modules/{module}/hidden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setModuleHidden(
            @PathVariable ModuleCode module,
            @RequestBody SetHiddenRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            menuCommandService.setModuleHidden(user.memberId(), module, request.hidden());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODULE_DISPLAY_NOT_FOUND");
        }
    }
}
