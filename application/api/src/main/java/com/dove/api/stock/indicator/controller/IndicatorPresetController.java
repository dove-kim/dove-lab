package com.dove.api.stock.indicator.controller;

import com.dove.api.stock.indicator.dto.CreateIndicatorPresetRequest;
import com.dove.api.stock.indicator.dto.IndicatorPresetResponse;
import com.dove.api.stock.indicator.dto.PresetReorderRequest;
import com.dove.api.stock.indicator.dto.UpdateIndicatorPresetRequest;
import com.dove.security.AuthenticatedUser;
import com.dove.screening.application.service.IndicatorPresetCommandService;
import com.dove.screening.application.service.IndicatorPresetQueryService;
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

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/indicator-presets")
@RequiredArgsConstructor
public class IndicatorPresetController {

    private final IndicatorPresetCommandService commandService;
    private final IndicatorPresetQueryService   queryService;

    @GetMapping
    public List<IndicatorPresetResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return queryService.findAllByMemberId(user.memberId()).stream()
                .map(IndicatorPresetResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndicatorPresetResponse create(
            @RequestBody @Valid CreateIndicatorPresetRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return IndicatorPresetResponse.from(
                    commandService.create(user.memberId(), req.name(),
                            req.items().toString(), toPanelOrderString(req.panelOrder())));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PRESET_NAME_DUPLICATE");
        }
    }

    @PutMapping("/{id}")
    public IndicatorPresetResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateIndicatorPresetRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return IndicatorPresetResponse.from(
                    commandService.update(user.memberId(), id, req.name(),
                            req.items().toString(), toPanelOrderString(req.panelOrder())));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PRESET_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PRESET_NAME_DUPLICATE");
        }
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody PresetReorderRequest request,
                        @AuthenticationPrincipal AuthenticatedUser user) {
        commandService.reorder(user.memberId(), request.ids());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            commandService.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PRESET_NOT_FOUND");
        }
    }

    private String toPanelOrderString(List<String> panelOrder) {
        if (panelOrder == null || panelOrder.isEmpty()) return null;
        return String.join(",", panelOrder);
    }
}
