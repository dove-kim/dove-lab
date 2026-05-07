package com.dove.stockapi.domain.stockset;

import com.dove.member.application.service.MemberQueryService;
import com.dove.stockapi.domain.stockset.dto.CreateStockSetRequest;
import com.dove.stockapi.domain.stockset.dto.StockSetResponse;
import com.dove.stockapi.domain.stockset.dto.UpdateStockSetRequest;
import com.dove.stockfilter.application.service.StockSetCommandService;
import com.dove.stockfilter.application.service.StockSetQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/stock-filters")
@RequiredArgsConstructor
public class StockSetController {

    private final StockSetCommandService stockSetCommandService;
    private final StockSetQueryService stockSetQueryService;
    private final MemberQueryService memberQueryService;

    @GetMapping
    public List<StockSetResponse> list(@AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        return stockSetQueryService.findAllByMemberId(memberId).stream()
                .map(StockSetResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public StockSetResponse get(@PathVariable Long id, @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        return stockSetQueryService.findByIdAndMemberId(id, memberId)
                .map(StockSetResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockSetResponse create(@RequestBody @Valid CreateStockSetRequest request,
                                    @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return StockSetResponse.from(
                    stockSetCommandService.create(memberId, request.name(), request.codes()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_SET_NAME_DUPLICATE");
        }
    }

    @PutMapping("/{id}")
    public StockSetResponse update(@PathVariable Long id,
                                    @RequestBody @Valid UpdateStockSetRequest request,
                                    @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return StockSetResponse.from(
                    stockSetCommandService.update(memberId, id, request.name(), request.codes()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_SET_NAME_DUPLICATE");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            stockSetCommandService.delete(memberId, id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_SET_NOT_FOUND");
        }
    }

    private Long resolveMemberId(String username) {
        return memberQueryService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }
}
