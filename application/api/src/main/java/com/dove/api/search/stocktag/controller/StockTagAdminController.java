package com.dove.api.search.stocktag.controller;

import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.search.stocktag.dto.UpdateTagLabelRequest;
import com.dove.stock.application.service.StockTagValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * 종목 분류 값 표시명 관리 API (ROOT 전용).
 */
@RestController
@RequestMapping("/admin/stock-tags")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class StockTagAdminController {

    private final StockTagValueService commandService;

    /**
     * 분류 값의 표시 라벨을 수정한다.
     */
    @PatchMapping("/{id}/label")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLabel(@PathVariable Long id, @RequestBody @Valid UpdateTagLabelRequest req) {
        try {
            commandService.updateLabel(id, req.label());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STOCK_TAG_VALUE_NOT_FOUND");
        }
    }
}
