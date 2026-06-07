package com.dove.api.member.invite.controller;

import com.dove.api.member.invite.dto.CreateInviteCodeRequest;
import com.dove.api.member.invite.dto.InviteCodeResponse;
import com.dove.auth.application.service.InviteCodeService;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원가입 초대 코드 발급·조회 API.
 */
@RestController
@RequestMapping("/admin/invite-codes")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class InviteCodeController {

    private final InviteCodeService inviteCodeService;

    /**
     * 권한 등급·만료 일수를 지정해 초대 코드를 생성한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponse createInviteCode(
            @RequestBody @Valid CreateInviteCodeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        InviteCode code = inviteCodeService.create(
                request.role(),
                LocalDateTime.now().plusDays(request.expireDays()),
                user.username());
        return InviteCodeResponse.from(code);
    }

    /**
     * 전체 초대 코드 목록을 반환한다.
     */
    @GetMapping
    public List<InviteCodeResponse> listInviteCodes() {
        return inviteCodeService.findAll().stream()
                .map(InviteCodeResponse::from)
                .toList();
    }
}
