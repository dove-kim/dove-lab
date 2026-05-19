package com.dove.api.stock.set.controller;

import com.dove.api.stock.set.dto.CreateStockSetRequest;
import com.dove.api.stock.set.dto.StockSetResponse;
import com.dove.api.stock.set.dto.UpdateStockSetRequest;
import com.dove.security.AuthenticatedUser;
import com.dove.screening.application.service.StockSetCommandService;
import com.dove.screening.application.service.StockSetQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

@RestController
@RequestMapping("/stock-sets")
@RequiredArgsConstructor
public class StockSetController {

    private final StockSetCommandService stockSetCommandService;
    private final StockSetQueryService stockSetQueryService;

    @GetMapping
    public List<StockSetResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return stockSetQueryService.findAllByMemberId(user.memberId()).stream()
                .map(StockSetResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public StockSetResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return stockSetQueryService.findByIdAndMemberId(id, user.memberId())
                .map(StockSetResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockSetResponse create(@RequestBody @Valid CreateStockSetRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockSetResponse.from(
                    stockSetCommandService.create(user.memberId(), request.name(), request.codes()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_SET_NAME_DUPLICATE");
        }
    }

    @PutMapping("/{id}")
    public StockSetResponse update(@PathVariable Long id,
                                   @RequestBody @Valid UpdateStockSetRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return StockSetResponse.from(
                    stockSetCommandService.update(user.memberId(), id, request.name(), request.codes()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_SET_NAME_DUPLICATE");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            stockSetCommandService.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND");
        }
    }
}
