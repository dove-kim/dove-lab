package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioRoundTripResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.api.portfolio.service.PortfolioPositionService;
import com.dove.api.portfolio.service.PortfolioRoundTripService;
import com.dove.api.portfolio.service.PortfolioSummaryService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 본인 포트폴리오의 요약·보유 포지션·청산 성과 조회 API.
 */
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioController {

    private final PortfolioSummaryService summaryService;
    private final PortfolioPositionService positionService;
    private final PortfolioRoundTripService roundTripService;

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return summaryService.compute(user.memberId());
    }

    @GetMapping("/positions")
    public List<PortfolioPositionResponse> positions(@AuthenticationPrincipal AuthenticatedUser user) {
        return positionService.compute(user.memberId());
    }

    @GetMapping("/roundtrips")
    public List<PortfolioRoundTripResponse> roundtrips(@AuthenticationPrincipal AuthenticatedUser user) {
        return roundTripService.compute(user.memberId());
    }
}
