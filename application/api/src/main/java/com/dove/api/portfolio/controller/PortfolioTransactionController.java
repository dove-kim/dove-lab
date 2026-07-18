package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.CreatePortfolioTransactionRequest;
import com.dove.api.portfolio.dto.PortfolioTransactionResponse;
import com.dove.api.portfolio.dto.UpdatePortfolioTransactionRequest;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 포트폴리오 거래 관리 API.
 */
@RestController
@RequestMapping("/portfolio/transactions")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioTransactionController {

    private final PortfolioTransactionService service;
    private final PortfolioAccountService accountService;

    @GetMapping
    public List<PortfolioTransactionResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        Map<Long, String> accountNames = accountService.findByOwner(user.memberId()).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        return service.findByOwner(user.memberId()).stream()
                .map(t -> PortfolioTransactionResponse.of(t, accountNames.get(t.getAccountId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioTransactionResponse create(@RequestBody @Valid CreatePortfolioTransactionRequest req,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var t = service.create(user.memberId(), req.accountId(), req.type(), req.tradedAt(), req.symbol(),
                    req.currency(), req.quantity(), req.price(), req.amount(), req.fee(),
                    req.tag(), req.memo(), user.username());
            return PortfolioTransactionResponse.of(t, accountName(user.memberId(), t.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
    }

    @PutMapping("/{id}")
    public PortfolioTransactionResponse update(@PathVariable Long id,
                                             @RequestBody @Valid UpdatePortfolioTransactionRequest req,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            var t = service.update(user.memberId(), id, req.type(), req.tradedAt(), req.symbol(), req.currency(),
                    req.quantity(), req.price(), req.amount(), req.fee(),
                    req.tag(), req.memo(), user.username());
            return PortfolioTransactionResponse.of(t, accountName(user.memberId(), t.getAccountId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_TRANSACTION_NOT_FOUND");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            service.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_TRANSACTION_NOT_FOUND");
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
