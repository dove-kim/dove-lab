package com.dove.stockapi.domain.indicatorpreset;

import com.dove.member.application.service.MemberQueryService;
import com.dove.stockapi.domain.indicatorpreset.dto.CreateIndicatorPresetRequest;
import com.dove.stockapi.domain.indicatorpreset.dto.IndicatorPresetResponse;
import com.dove.stockapi.domain.indicatorpreset.dto.UpdateIndicatorPresetRequest;
import com.dove.stockfilter.application.service.IndicatorPresetCommandService;
import com.dove.stockfilter.application.service.IndicatorPresetQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

record PresetReorderRequest(List<Long> ids) {}

@RestController
@RequestMapping("/api/indicator-presets")
@RequiredArgsConstructor
public class IndicatorPresetController {

    private final IndicatorPresetCommandService commandService;
    private final IndicatorPresetQueryService   queryService;
    private final MemberQueryService            memberQueryService;

    @GetMapping
    public List<IndicatorPresetResponse> list(@AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        return queryService.findAllByMemberId(memberId).stream()
                .map(IndicatorPresetResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndicatorPresetResponse create(
            @RequestBody @Valid CreateIndicatorPresetRequest req,
            @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return IndicatorPresetResponse.from(
                    commandService.create(memberId, req.name(),
                            req.items().toString(), toPanelOrderString(req.panelOrder())));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PRESET_NAME_DUPLICATE");
        }
    }

    @PutMapping("/{id}")
    public IndicatorPresetResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateIndicatorPresetRequest req,
            @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            return IndicatorPresetResponse.from(
                    commandService.update(memberId, id, req.name(),
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
                        @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        commandService.reorder(memberId, request.ids());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal String username) {
        Long memberId = resolveMemberId(username);
        try {
            commandService.delete(memberId, id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PRESET_NOT_FOUND");
        }
    }

    private Long resolveMemberId(String username) {
        return memberQueryService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();
    }

    private String toPanelOrderString(List<String> panelOrder) {
        if (panelOrder == null || panelOrder.isEmpty()) return null;
        return panelOrder.stream().collect(Collectors.joining(","));
    }
}
