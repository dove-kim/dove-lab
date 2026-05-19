package com.dove.api.admin.invite.controller;

import com.dove.api.admin.invite.dto.CreateInviteCodeRequest;
import com.dove.api.admin.invite.dto.InviteCodeResponse;
import com.dove.auth.application.service.InviteCodeCommandService;
import com.dove.auth.application.service.InviteCodeQueryService;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/root")
@RequiredArgsConstructor
public class InviteCodeController {

    private final InviteCodeCommandService inviteCodeCommandService;
    private final InviteCodeQueryService inviteCodeQueryService;

    @PostMapping("/invite-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponse createInviteCode(
            @RequestBody @Valid CreateInviteCodeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        InviteCode code = inviteCodeCommandService.create(
                request.role(),
                LocalDateTime.now().plusDays(request.expireDays()),
                user.username());
        return InviteCodeResponse.from(code);
    }

    @GetMapping("/invite-codes")
    public List<InviteCodeResponse> listInviteCodes() {
        return inviteCodeQueryService.findAll().stream()
                .map(InviteCodeResponse::from)
                .toList();
    }
}
