package com.dove.api.search.indicatorpreset.controller;

import com.dove.api.search.indicatorpreset.dto.CreateIndicatorPresetRequest;
import com.dove.api.search.indicatorpreset.dto.IndicatorPresetResponse;
import com.dove.api.search.indicatorpreset.dto.PresetReorderRequest;
import com.dove.api.search.indicatorpreset.dto.UpdateIndicatorPresetRequest;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.userfeature.domain.capability.Capability;
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

/**
 * 회원별 차트 지표 프리셋 관리 API.
 */
@RestController
@RequestMapping("/indicator-presets")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_VIEW)
public class IndicatorPresetController {

    private final IndicatorPresetCommandService commandService;
    private final IndicatorPresetQueryService   queryService;

    /**
     * 현재 회원의 지표 프리셋 목록을 반환한다.
     */
    @GetMapping
    public List<IndicatorPresetResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return queryService.findAllByMemberId(user.memberId()).stream()
                .map(IndicatorPresetResponse::from)
                .toList();
    }

    /**
     * 새 지표 프리셋을 생성한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndicatorPresetResponse create(
            @RequestBody @Valid CreateIndicatorPresetRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return IndicatorPresetResponse.from(
                    commandService.create(user.memberId(), req.name(), req.items(), req.panelOrder()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PRESET_NAME_DUPLICATE");
        }
    }

    /**
     * 기존 지표 프리셋을 수정한다.
     */
    @PutMapping("/{id}")
    public IndicatorPresetResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateIndicatorPresetRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return IndicatorPresetResponse.from(
                    commandService.update(user.memberId(), id, req.name(), req.items(), req.panelOrder()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PRESET_NOT_FOUND");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PRESET_NAME_DUPLICATE");
        }
    }

    /**
     * 지표 프리셋 표시 순서를 변경한다.
     */
    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@RequestBody PresetReorderRequest request,
                        @AuthenticationPrincipal AuthenticatedUser user) {
        commandService.reorder(user.memberId(), request.ids());
    }

    /**
     * 지표 프리셋을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            commandService.delete(user.memberId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PRESET_NOT_FOUND");
        }
    }
}
