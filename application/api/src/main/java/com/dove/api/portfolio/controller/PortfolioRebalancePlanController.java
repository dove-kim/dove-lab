package com.dove.api.portfolio.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.PortfolioRebalancePlanResponse;
import com.dove.api.portfolio.dto.SavePortfolioRebalancePlanRequest;
import com.dove.portfolio.application.service.PortfolioRebalancePlanService;
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

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 리밸런싱 계획 관리 API — 저장(이름 기준 upsert)·조회·삭제.
 */
@RestController
@RequestMapping("/portfolio/rebalance-plans")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_REBALANCE)
public class PortfolioRebalancePlanController {

    private final PortfolioRebalancePlanService service;

    @GetMapping
    public List<PortfolioRebalancePlanResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.findByOwner(user.memberId()).stream()
                .map(PortfolioRebalancePlanResponse::of)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioRebalancePlanResponse save(@RequestBody @Valid SavePortfolioRebalancePlanRequest req,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        var p = service.save(user.memberId(), req.name().trim(), req.entries(), user.username());
        return PortfolioRebalancePlanResponse.of(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            service.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PORTFOLIO_REBALANCE_PLAN_NOT_FOUND");
        }
    }
}
