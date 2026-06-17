package com.dove.api.search.searchfilter.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.search.searchfilter.dto.CreateSearchFilterRequest;
import com.dove.api.search.searchfilter.dto.ExecuteFilterRequest;
import com.dove.api.search.searchfilter.dto.ExecuteFilterResponse;
import com.dove.api.search.searchfilter.dto.FilterReorderRequest;
import com.dove.api.search.searchfilter.dto.SearchFilterResponse;
import com.dove.api.search.searchfilter.dto.UpdateSearchFilterRequest;
import com.dove.api.search.searchfilter.service.FilterExecutionService;
import com.dove.screening.application.service.SearchFilterCommandService;
import com.dove.screening.application.service.SearchFilterQueryService;
import com.dove.screening.domain.entity.SearchFilter;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.userfeature.domain.capability.Capability;
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

/**
 * 사용자 정의 검색 필터 관리 및 실행 API.
 */
@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_SEARCH)
public class SearchFilterController {

    private final SearchFilterCommandService searchFilterCommandService;
    private final SearchFilterQueryService searchFilterQueryService;
    private final FilterExecutionService filterExecutionService;

    /**
     * 현재 회원의 검색 필터 목록을 반환한다.
     */
    @GetMapping
    public List<SearchFilterResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return searchFilterQueryService.findAllByMemberId(user.memberId()).stream()
                .map(SearchFilterResponse::from)
                .toList();
    }

    /**
     * 새 검색 필터를 생성한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchFilterResponse create(@RequestBody @Valid CreateSearchFilterRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return SearchFilterResponse.from(searchFilterCommandService.create(
                    user.memberId(), request.name(), request.dateRule(),
                    request.markets(), request.priceType(), request.exchange(),
                    FilterExpression.parse(request.expression()),
                    request.stockFilterId()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FILTER_NAME_DUPLICATE");
        }
    }

    /**
     * 기존 검색 필터를 수정한다.
     */
    @PutMapping("/{id}")
    public SearchFilterResponse update(@PathVariable Long id,
                                       @RequestBody @Valid UpdateSearchFilterRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return SearchFilterResponse.from(searchFilterCommandService.update(
                    user.memberId(), id, request.name(), request.dateRule(),
                    request.markets(), request.priceType(), request.exchange(),
                    FilterExpression.parse(request.expression()),
                    request.stockFilterId()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FILTER_NAME_DUPLICATE");
        }
    }

    /**
     * 검색 필터를 삭제한다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            searchFilterCommandService.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND");
        }
    }

    /**
     * 검색 필터 표시 순서를 변경한다.
     */
    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody FilterReorderRequest request,
                        @AuthenticationPrincipal AuthenticatedUser user) {
        searchFilterCommandService.reorder(user.memberId(), request.ids());
    }

    /**
     * 검색 필터를 실행해 조건에 맞는 종목 결과를 반환한다.
     */
    @PostMapping("/{id}/execute")
    public ExecuteFilterResponse execute(@PathVariable Long id,
                                         @RequestBody(required = false) ExecuteFilterRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        SearchFilter filter = searchFilterQueryService.findByIdAndMemberId(id, user.memberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FILTER_NOT_FOUND"));
        LocalDate referenceDate = request != null ? request.referenceDate() : null;
        return ExecuteFilterResponse.from(filter, filterExecutionService.execute(filter, referenceDate));
    }
}
