package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioRoundTripResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.api.portfolio.dto.PortfolioTransactionResponse;
import com.dove.api.portfolio.service.PortfolioAllService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * "전체"(내 소유 + 공유받은 계좌 합산) 조회 API.
 */
@RestController
@RequestMapping("/portfolio/all")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioAllController {

    private final PortfolioAllService service;

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.summary(user.memberId());
    }

    @GetMapping("/positions")
    public List<PortfolioPositionResponse> positions(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.positions(user.memberId());
    }

    @GetMapping("/transactions")
    public List<PortfolioTransactionResponse> transactions(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.transactions(user.memberId());
    }

    @GetMapping("/roundtrips")
    public List<PortfolioRoundTripResponse> roundtrips(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.roundtrips(user.memberId());
    }
}
