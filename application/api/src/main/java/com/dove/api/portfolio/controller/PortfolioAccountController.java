package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.CreatePortfolioAccountRequest;
import com.dove.api.portfolio.dto.PortfolioAccountResponse;
import com.dove.api.portfolio.dto.UpdatePortfolioAccountRequest;
import com.dove.portfolio.application.exception.DuplicatePortfolioAccountNameException;
import com.dove.portfolio.application.service.PortfolioAccountService;
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
import java.util.NoSuchElementException;

/**
 * 포트폴리오 계좌 관리 API.
 */
@RestController
@RequestMapping("/portfolio/accounts")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioAccountController {

    private final PortfolioAccountService service;

    @GetMapping
    public List<PortfolioAccountResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.findByOwner(user.memberId()).stream().map(PortfolioAccountResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioAccountResponse create(@RequestBody @Valid CreatePortfolioAccountRequest req,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return PortfolioAccountResponse.from(
                    service.create(user.memberId(), req.name(), req.brokerName(), req.description(), user.username()));
        } catch (DuplicatePortfolioAccountNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_PORTFOLIO_ACCOUNT_NAME");
        }
    }

    @PutMapping("/{id}")
    public PortfolioAccountResponse update(@PathVariable Long id,
                                         @RequestBody @Valid UpdatePortfolioAccountRequest req,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return PortfolioAccountResponse.from(
                    service.update(user.memberId(), id, req.name(), req.brokerName(), req.description(), user.username()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        } catch (DuplicatePortfolioAccountNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_PORTFOLIO_ACCOUNT_NAME");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            service.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_ACCOUNT_NOT_FOUND");
        }
    }
}
