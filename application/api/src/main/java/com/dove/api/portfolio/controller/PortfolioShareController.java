package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.GrantPortfolioShareRequest;
import com.dove.api.portfolio.dto.PortfolioShareResponse;
import com.dove.auth.application.service.CredentialService;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioShareService;
import com.dove.portfolio.domain.entity.PortfolioShare;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 포트폴리오 계좌 공유 관리 API — 내보낸/받은 공유 조회, 공유 부여·회수.
 */
@RestController
@RequestMapping("/portfolio/shares")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioShareController {

    private final PortfolioShareService shareService;
    private final PortfolioAccountService accountService;
    private final CredentialService credentialService;
    private final MemberProfileQueryService memberProfileQueryService;

    @GetMapping
    public List<PortfolioShareResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        List<PortfolioShareResponse> result = new ArrayList<>();
        for (PortfolioShare s : shareService.listByOwner(user.memberId())) {
            result.add(new PortfolioShareResponse(s.getId(), s.getAccountId(), accountName(s.getAccountId()),
                    display(s.getGranteeMemberId()), s.getPermission().name(), "OUT"));
        }
        for (PortfolioShare s : shareService.listByGrantee(user.memberId())) {
            result.add(new PortfolioShareResponse(s.getId(), s.getAccountId(), accountName(s.getAccountId()),
                    display(s.getOwnerMemberId()), s.getPermission().name(), "IN"));
        }
        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioShareResponse grant(@RequestBody @Valid GrantPortfolioShareRequest req,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        long granteeId = credentialService.findByUsername(req.granteeUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_GRANTEE_NOT_FOUND"))
                .getMemberId();
        if (granteeId == user.memberId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANNOT_SHARE_TO_SELF");
        }
        // 소유자만 공유 가능 — 소유하지 않으면 404
        try {
            accountService.getOwned(user.memberId(), req.accountId());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
        PortfolioShare s = shareService.grant(user.memberId(), req.accountId(), granteeId,
                req.permission(), user.username());
        return new PortfolioShareResponse(s.getId(), s.getAccountId(), accountName(s.getAccountId()),
                display(s.getGranteeMemberId()), s.getPermission().name(), "OUT");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            shareService.revoke(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_SHARE_NOT_FOUND");
        }
    }

    private String accountName(Long accountId) {
        try {
            return accountService.getById(accountId).getName();
        } catch (NoSuchElementException e) {
            return "?";
        }
    }

    /** 회원 표시명 "이름 (아이디)". */
    private String display(Long memberId) {
        String name = memberProfileQueryService.findById(memberId).map(p -> p.getName()).orElse("회원");
        String username = credentialService.findByMemberId(memberId).map(c -> c.getUsername()).orElse(String.valueOf(memberId));
        return name + " (" + username + ")";
    }
}
