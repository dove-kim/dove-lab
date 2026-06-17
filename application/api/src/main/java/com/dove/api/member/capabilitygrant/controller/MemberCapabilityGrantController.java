package com.dove.api.member.capabilitygrant.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.member.capabilitygrant.dto.UpdateUserCapabilityRequest;
import com.dove.userfeature.application.service.MemberCapabilityGrantCommandService;
import com.dove.userfeature.application.service.MemberCapabilityGrantQueryService;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 회원 capability 권한 부여 관리 API.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class MemberCapabilityGrantController {

    private final MemberCapabilityGrantCommandService commandService;
    private final MemberCapabilityGrantQueryService queryService;

    /**
     * 회원에게 부여된 capability 집합을 조회한다.
     */
    @GetMapping("/{userId}/capabilities")
    public Set<Capability> getCapabilities(@PathVariable("userId") Long memberId) {
        return queryService.findGrantedCapabilities(memberId);
    }

    /**
     * 회원에게 capability를 부여하거나 회수한다.
     */
    @PatchMapping("/{userId}/capabilities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCapability(
            @PathVariable("userId") Long memberId,
            @RequestBody @Valid UpdateUserCapabilityRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> commandService.grant(memberId, request.capability(), admin.memberId());
            case REVOKE -> commandService.revoke(memberId, request.capability());
        }
    }
}
