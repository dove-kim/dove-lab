package com.dove.stockapi.domain.filter;

import com.dove.market.domain.enums.MarketType;
import com.dove.member.application.service.MemberQueryService;
import com.dove.stockapi.domain.filter.dto.*;
import com.dove.stockfilter.application.service.SearchFilterCommandService;
import com.dove.stockfilter.application.service.SearchFilterQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

record FilterReorderRequest(List<Long> ids) {}

@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class SearchFilterController {

    private final SearchFilterCommandService searchFilterCommandService;
    private final SearchFilterQueryService searchFilterQueryService;
    private final FilterExecutionService filterExecutionService;
    private final MemberQueryService memberQueryService;

    @GetMapping
    public List<SearchFilterResponse> list(@AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        return searchFilterQueryService.findAllByMemberId(memberId).stream()
                .map(SearchFilterResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchFilterResponse create(
            @RequestBody @Valid CreateSearchFilterRequest request,
            @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return SearchFilterResponse.from(
                    searchFilterCommandService.create(memberId, request.name(), request.dateRule(),
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
            @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return SearchFilterResponse.from(
                    searchFilterCommandService.update(memberId, id, request.name(), request.dateRule(),
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
    public void delete(@PathVariable Long id, @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            searchFilterCommandService.delete(memberId, id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND");
        }
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody FilterReorderRequest request,
                        @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        searchFilterCommandService.reorder(memberId, request.ids());
    }

    @PostMapping("/{id}/execute")
    public ExecuteFilterResponse execute(
            @PathVariable Long id,
            @RequestBody(required = false) ExecuteFilterRequest request,
            @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        LocalDate referenceDate = request != null ? request.referenceDate() : null;
        return filterExecutionService.execute(memberId, id, referenceDate);
    }

    private Long resolveMemberId(String username) {
        return memberQueryService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }

    private List<MarketType> parseMarkets(List<String> markets) {
        return markets.stream().map(MarketType::valueOf).toList();
    }
}
