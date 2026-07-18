package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.AttachPortfolioHoldingRequest;
import com.dove.api.portfolio.dto.PortfolioHoldingResponse;
import com.dove.api.portfolio.dto.SetHoldingDividendRequest;
import com.dove.api.portfolio.dto.SetHoldingTrackingRequest;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioHoldingService;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 포트폴리오 종목 식별(시장·티커) 관리 API.
 */
@Slf4j
@RestController
@RequestMapping("/portfolio/holdings")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioHoldingController {

    private final PortfolioHoldingService service;
    private final PortfolioAccountService accountService;
    private final PortfolioQuoteService quoteService;
    private final PortfolioFxRateService fxRateService;

    @GetMapping
    public List<PortfolioHoldingResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        Map<Long, String> accountNames = accountService.findByOwner(user.memberId()).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        return service.findByOwner(user.memberId()).stream()
                .map(h -> PortfolioHoldingResponse.of(h, accountNames.get(h.getAccountId())))
                .toList();
    }

    @PostMapping
    public PortfolioHoldingResponse attach(@RequestBody @Valid AttachPortfolioHoldingRequest req,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var h = service.attach(user.memberId(), req.accountId(), req.symbol(), req.market(), req.ticker(),
                    user.username());
            fetchOnEntry(h);
            return PortfolioHoldingResponse.of(h, accountName(user.memberId(), h.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
    }

    /**
     * 종목 첫 진입 시 현재가·환율을 즉시 1회 확보한다(best-effort — 실패해도 등록은 성공, 일배치가 보정).
     */
    private void fetchOnEntry(PortfolioHolding h) {
        PortfolioMarket market = h.getMarket();
        if (market.isDomestic()) {
            return;
        }
        try {
            quoteService.refreshOne(market, h.getTicker());
            fxRateService.refreshOne(market.getCurrency());
        } catch (RuntimeException e) {
            log.warn("On-entry market data fetch failed for {} {}: {}", market, h.getTicker(), e.getMessage());
        }
    }

    @PutMapping("/{id}/dividend")
    public PortfolioHoldingResponse setDividend(@PathVariable Long id,
                                                @RequestBody @Valid SetHoldingDividendRequest req,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var h = service.setDividend(user.memberId(), id, req.annualDividendPct(), user.username());
            return PortfolioHoldingResponse.of(h, accountName(user.memberId(), h.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_HOLDING_NOT_FOUND");
        }
    }

    @PutMapping("/{id}/tracking")
    public PortfolioHoldingResponse setTracking(@PathVariable Long id,
                                                @RequestBody @Valid SetHoldingTrackingRequest req,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var h = service.setTracking(user.memberId(), id, req.tracked(), user.username());
            return PortfolioHoldingResponse.of(h, accountName(user.memberId(), h.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_HOLDING_NOT_FOUND");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            service.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_HOLDING_NOT_FOUND");
        }
    }

    private String accountName(Long memberId, Long accountId) {
        return accountService.findByOwner(memberId).stream()
                .filter(a -> a.getId().equals(accountId))
                .map(PortfolioAccount::getName)
                .findFirst()
                .orElse(null);
    }
}
