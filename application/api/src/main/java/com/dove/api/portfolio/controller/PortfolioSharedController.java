package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.CreatePortfolioTransactionRequest;
import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioRoundTripResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.api.portfolio.dto.PortfolioTransactionResponse;
import com.dove.api.portfolio.service.PortfolioAccess;
import com.dove.api.portfolio.service.PortfolioAccessService;
import com.dove.api.portfolio.service.PortfolioPositionService;
import com.dove.api.portfolio.service.PortfolioRoundTripService;
import com.dove.api.portfolio.service.PortfolioSummaryService;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공유받은 계좌 열람·쓰기 API — 접근 권한(소유 또는 공유)을 확인하고 대상 계좌로 한정해 조회한다.
 * READ_RELATIVE 권한은 금액을 숨기고 비율·수익률만 노출한다.
 */
@RestController
@RequestMapping("/portfolio/shared/{accountId}")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioSharedController {

    private final PortfolioAccessService accessService;
    private final PortfolioSummaryService summaryService;
    private final PortfolioPositionService positionService;
    private final PortfolioRoundTripService roundTripService;
    private final PortfolioTransactionService transactionService;
    private final PortfolioAccountService accountService;

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(@PathVariable Long accountId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioAccess access = accessService.resolveRead(user.memberId(), accountId);
        PortfolioSummaryResponse s = summaryService.compute(access.ownerMemberId(), accountId);
        return access.hideAmounts()
                ? new PortfolioSummaryResponse(0, 0, 0, 0, 0, s.evalPnlPct(), s.xirrPct(), java.util.Map.of())
                : s;
    }

    @GetMapping("/positions")
    public List<PortfolioPositionResponse> positions(@PathVariable Long accountId,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioAccess access = accessService.resolveRead(user.memberId(), accountId);
        List<PortfolioPositionResponse> positions = positionService.compute(access.ownerMemberId(), accountId);
        return access.hideAmounts() ? positions.stream().map(this::stripAmounts).toList() : positions;
    }

    @GetMapping("/transactions")
    public List<PortfolioTransactionResponse> transactions(@PathVariable Long accountId,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioAccess access = accessService.resolveRead(user.memberId(), accountId);
        String name = accountService.getById(accountId).getName();
        return transactionService.findByOwner(access.ownerMemberId()).stream()
                .filter(t -> accountId.equals(t.getAccountId()))
                .map(t -> access.hideAmounts()
                        ? stripAmounts(PortfolioTransactionResponse.of(t, name))
                        : PortfolioTransactionResponse.of(t, name))
                .toList();
    }

    @GetMapping("/roundtrips")
    public List<PortfolioRoundTripResponse> roundtrips(@PathVariable Long accountId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioAccess access = accessService.resolveRead(user.memberId(), accountId);
        List<PortfolioRoundTripResponse> trips = roundTripService.compute(access.ownerMemberId(), accountId);
        return access.hideAmounts() ? trips.stream().map(this::stripAmounts).toList() : trips;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioTransactionResponse addTransaction(@PathVariable Long accountId,
                                                       @RequestBody @Valid CreatePortfolioTransactionRequest req,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioAccess access = accessService.resolveWrite(user.memberId(), accountId);
        String name = accountService.getById(accountId).getName();
        var t = transactionService.create(access.ownerMemberId(), accountId, req.type(), req.tradedAt(), req.symbol(),
                req.currency(), req.quantity(), req.price(), req.amount(), req.fee(),
                req.tag(), req.memo(), user.username());
        return PortfolioTransactionResponse.of(t, name);
    }

    private PortfolioPositionResponse stripAmounts(PortfolioPositionResponse p) {
        return new PortfolioPositionResponse(p.symbol(), p.account(), p.currency(), p.tag(),
                null, null, null, 0L, 0L, p.pnlPct(), p.weightPct(), p.holdingId(), p.annualDividendPct(),
                p.dividendTracked());
    }

    private PortfolioTransactionResponse stripAmounts(PortfolioTransactionResponse t) {
        return new PortfolioTransactionResponse(t.id(), t.accountId(), t.account(), t.type(), t.tradedAt(),
                t.symbol(), t.currency(), null, null, null, null, t.tag(), t.memo());
    }

    private PortfolioRoundTripResponse stripAmounts(PortfolioRoundTripResponse t) {
        return new PortfolioRoundTripResponse(t.id(), t.symbol(), t.currency(), t.group(), t.entry(), t.exit(),
                t.holdingDays(), null, null, null, 0L, t.pnlPct(), t.open());
    }
}
