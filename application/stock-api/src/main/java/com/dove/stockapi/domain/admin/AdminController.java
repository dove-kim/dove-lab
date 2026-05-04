package com.dove.stockapi.domain.admin;

import com.dove.member.application.service.InviteCodeCommandService;
import com.dove.member.application.service.InviteCodeQueryService;
import com.dove.member.domain.entity.InviteCode;
import com.dove.stockapi.domain.admin.dto.CreateInviteCodeRequest;
import com.dove.stockapi.domain.admin.dto.InviteCodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InviteCodeCommandService inviteCodeCommandService;
    private final InviteCodeQueryService inviteCodeQueryService;

    @PostMapping("/invite-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponse createInviteCode(
            @RequestBody @Valid CreateInviteCodeRequest request,
            @AuthenticationPrincipal String currentUsername) {
        InviteCode code = inviteCodeCommandService.create(
                request.role(),
                LocalDateTime.now().plusDays(request.expireDays()),
                currentUsername);
        return InviteCodeResponse.from(code);
    }

    @GetMapping("/invite-codes")
    public List<InviteCodeResponse> listInviteCodes() {
        return inviteCodeQueryService.findAll().stream()
                .map(InviteCodeResponse::from)
                .toList();
    }
}
