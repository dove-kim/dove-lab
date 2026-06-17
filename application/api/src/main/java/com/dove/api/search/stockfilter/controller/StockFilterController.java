package com.dove.api.search.stockfilter.controller;

import com.dove.api.search.stockfilter.dto.CreateStockFilterRequest;
import com.dove.api.search.stockfilter.dto.PreviewTagRequest;
import com.dove.api.search.stockfilter.dto.StockFilterResponse;
import com.dove.api.search.stockfilter.dto.StockSummaryResponse;
import com.dove.api.search.stockfilter.dto.UpdateStockFilterRequest;
import com.dove.screening.application.exception.DuplicateStockFilterNameException;
import com.dove.screening.application.service.StockFilterCommandService;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.userfeature.domain.capability.Capability;
import com.dove.stock.application.service.StockQueryService;
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
import java.util.Set;

/**
 * 종목 필터 API (인증 회원).
 */
@RestController
@RequestMapping("/stock-filters")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_SEARCH)
public class StockFilterController {

    private final StockFilterQueryService queryService;
    private final StockFilterCommandService commandService;
    private final StockQueryService stockQueryService;

    /**
     * 현재 회원이 사용 가능한 종목 필터 목록을 반환한다.
     */
    @GetMapping("/available")
    public List<StockFilterResponse> listAvailable(@AuthenticationPrincipal AuthenticatedUser user) {
        return queryService.findAvailableForMember(user.memberId()).stream()
                .map(StockFilterResponse::from)
                .toList();
    }

    /**
     * 시스템 종목 필터 목록을 반환한다(권한에 따라 비활성 포함).
     */
    @GetMapping("/system")
    public List<StockFilterResponse> listSystemFilters(@AuthenticationPrincipal AuthenticatedUser user) {
        boolean canSeeDisabled = "ADMIN".equals(user.role()) || "ROOT".equals(user.role());
        var filters = canSeeDisabled
                ? queryService.findSystemFilters()
                : queryService.findSystemFiltersEnabled();
        return filters.stream().map(StockFilterResponse::from).toList();
    }

    /**
     * 태그·수치 조건으로 매칭되는 종목을 미리보기한다.
     */
    @PostMapping("/preview/tag")
    public List<StockSummaryResponse> previewTag(@RequestBody PreviewTagRequest req) {
        Set<String> tickers = queryService.previewByTagConditions(
                req.tagConditions(), req.numericConditions(), req.namePatternConditions(), req.markets());
        Map<String, String> names = stockQueryService.findNamesByTickers(tickers);
        return stockQueryService.findByTickers(tickers).values().stream()
                .map(s -> StockSummaryResponse.from(s, names.getOrDefault(s.getTicker(), s.getTicker())))
                .toList();
    }

    /**
     * 현재 회원의 개인 종목 필터 목록을 반환한다.
     */
    @GetMapping("/personal")
    public List<StockFilterResponse> listPersonal(@AuthenticationPrincipal AuthenticatedUser user) {
        return queryService.findPersonalFilters(user.memberId()).stream()
                .map(StockFilterResponse::from)
                .toList();
    }

    /**
     * 개인 종목 필터를 생성한다.
     */
    @PostMapping("/personal")
    @ResponseStatus(HttpStatus.CREATED)
    public StockFilterResponse createPersonal(
            @RequestBody @Valid CreateStockFilterRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockFilterResponse.from(
                    commandService.createPersonal(user.memberId(), req.name(), req.description(),
                            req.tagConditions(), req.stockConditions(), req.numericConditions(),
                            req.namePatternConditions(), user.username()));
        } catch (DuplicateStockFilterNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_STOCK_FILTER_NAME");
        }
    }

    /**
     * 개인 종목 필터를 수정한다.
     */
    @PutMapping("/personal/{id}")
    public StockFilterResponse updatePersonal(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStockFilterRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockFilterResponse.from(
                    commandService.updatePersonal(user.memberId(), id, req.name(), req.description(),
                            req.tagConditions(), req.stockConditions(), req.numericConditions(),
                            req.namePatternConditions(), user.username()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_FILTER_NOT_FOUND");
        } catch (DuplicateStockFilterNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_STOCK_FILTER_NAME");
        }
    }

    /**
     * 개인 종목 필터를 삭제한다.
     */
    @DeleteMapping("/personal/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePersonal(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            commandService.deletePersonal(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_FILTER_NOT_FOUND");
        }
    }
}
