package com.dove.api.search.stockfilter.controller;

import com.dove.api.search.stockfilter.dto.CreateStockFilterRequest;
import com.dove.api.search.stockfilter.dto.SetEnabledRequest;
import com.dove.api.search.stockfilter.dto.StockFilterResponse;
import com.dove.api.search.stockfilter.dto.UpdateStockFilterRequest;
import com.dove.screening.application.exception.DuplicateStockFilterNameException;
import com.dove.screening.application.service.StockFilterCommandService;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * 시스템 종목 필터 관리 API (ADMIN 이상).
 */
@RestController
@RequestMapping("/admin/stock-filters/system")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class SystemStockFilterAdminController {

    private final StockFilterCommandService commandService;

    /**
     * 시스템 종목 필터를 생성한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockFilterResponse create(
            @RequestBody @Valid CreateStockFilterRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockFilterResponse.from(
                    commandService.createSystem(req.name(), req.description(),
                            req.tagConditions(), req.stockConditions(), req.numericConditions(), user.username()));
        } catch (DuplicateStockFilterNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_STOCK_FILTER_NAME");
        }
    }

    /**
     * 시스템 종목 필터를 수정한다.
     */
    @PutMapping("/{id}")
    public StockFilterResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStockFilterRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockFilterResponse.from(
                    commandService.updateSystem(id, req.name(), req.description(),
                            req.tagConditions(), req.stockConditions(), req.numericConditions(), user.username()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_FILTER_NOT_FOUND");
        } catch (DuplicateStockFilterNameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_STOCK_FILTER_NAME");
        }
    }

    /**
     * 시스템 종목 필터의 활성화 상태를 변경한다.
     */
    @PatchMapping("/{id}/enabled")
    public StockFilterResponse setEnabled(
            @PathVariable Long id,
            @RequestBody @Valid SetEnabledRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockFilterResponse.from(
                    commandService.setEnabled(id, req.enabled(), user.username()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_FILTER_NOT_FOUND");
        }
    }

    /**
     * 시스템 종목 필터를 삭제한다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        try {
            commandService.deleteSystem(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_FILTER_NOT_FOUND");
        }
    }
}
