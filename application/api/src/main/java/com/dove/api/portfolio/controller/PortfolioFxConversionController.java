package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.CreatePortfolioFxConversionRequest;
import com.dove.api.portfolio.dto.PortfolioFxConversionResponse;
import com.dove.api.portfolio.dto.UpdatePortfolioFxConversionRequest;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioFxConversionService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.userfeature.domain.capability.Capability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 포트폴리오 환전 관리 API.
 */
@RestController
@RequestMapping("/portfolio/fx-conversions")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioFxConversionController {

    private final PortfolioFxConversionService service;
    private final PortfolioAccountService accountService;

    @GetMapping
    public List<PortfolioFxConversionResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        Map<Long, String> accountNames = accountService.findByOwner(user.memberId()).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        return service.findByOwner(user.memberId()).stream()
                .map(c -> PortfolioFxConversionResponse.of(c, accountNames.get(c.getAccountId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioFxConversionResponse create(@RequestBody @Valid CreatePortfolioFxConversionRequest req,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var c = service.create(user.memberId(), req.accountId(), req.convDate(), req.fromCurrency(),
                    req.fromAmount(), req.toCurrency(), req.toAmount(), req.fee(), req.memo(), user.username());
            return PortfolioFxConversionResponse.of(c, accountName(user.memberId(), c.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
    }

    @PutMapping("/{id}")
    public PortfolioFxConversionResponse update(@PathVariable Long id,
                                                @RequestBody @Valid UpdatePortfolioFxConversionRequest req,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var c = service.update(user.memberId(), id, req.convDate(), req.fromCurrency(), req.fromAmount(),
                    req.toCurrency(), req.toAmount(), req.fee(), req.memo(), user.username());
            return PortfolioFxConversionResponse.of(c, accountName(user.memberId(), c.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_FX_CONVERSION_NOT_FOUND");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            service.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_FX_CONVERSION_NOT_FOUND");
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
