package com.dove.api.stock.filter.controller;

import com.dove.api.stock.filter.dto.CreateSearchFilterRequest;
import com.dove.api.stock.filter.dto.ExecuteFilterRequest;
import com.dove.api.stock.filter.dto.ExecuteFilterResponse;
import com.dove.api.stock.filter.dto.FilterReorderRequest;
import com.dove.api.stock.filter.dto.SearchFilterResponse;
import com.dove.api.stock.filter.dto.UpdateSearchFilterRequest;
import com.dove.api.stock.filter.service.FilterExecutionService;
import com.dove.market.domain.enums.MarketType;
import com.dove.security.AuthenticatedUser;
import com.dove.screening.application.service.SearchFilterCommandService;
import com.dove.screening.application.service.SearchFilterQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
public class SearchFilterController {

    private final SearchFilterCommandService searchFilterCommandService;
    private final SearchFilterQueryService searchFilterQueryService;
    private final FilterExecutionService filterExecutionService;

    @GetMapping
    public List<SearchFilterResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return searchFilterQueryService.findAllByMemberId(user.memberId()).stream()
                .map(SearchFilterResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchFilterResponse create(
            @RequestBody @Valid CreateSearchFilterRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return SearchFilterResponse.from(
                    searchFilterCommandService.create(user.memberId(), request.name(), request.dateRule(),
                            parseMarkets(request.markets()), request.expression(),
                            request.includeStockSetId(), request.excludeStockSetId()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FILTER_NAME_DUPLICATE");
        }
    }

    @PutMapping("/{id}")
    public SearchFilterResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSearchFilterRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return SearchFilterResponse.from(
                    searchFilterCommandService.update(user.memberId(), id, request.name(), request.dateRule(),
                            parseMarkets(request.markets()), request.expression(),
                            request.includeStockSetId(), request.excludeStockSetId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FILTER_NAME_DUPLICATE");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            searchFilterCommandService.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND");
        }
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody FilterReorderRequest request,
                        @AuthenticationPrincipal AuthenticatedUser user) {
        searchFilterCommandService.reorder(user.memberId(), request.ids());
    }

    @PostMapping("/{id}/execute")
    public ExecuteFilterResponse execute(
            @PathVariable Long id,
            @RequestBody(required = false) ExecuteFilterRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        LocalDate referenceDate = request != null ? request.referenceDate() : null;
        return filterExecutionService.execute(user.memberId(), id, referenceDate);
    }

    private List<MarketType> parseMarkets(List<String> markets) {
        return markets.stream().map(MarketType::valueOf).toList();
    }
}
