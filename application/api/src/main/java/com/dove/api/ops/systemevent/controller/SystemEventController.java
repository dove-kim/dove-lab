package com.dove.api.ops.systemevent.controller;

import com.dove.api.global.dto.PageResponse;
import com.dove.api.ops.systemevent.dto.SystemEventResponse;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.systemevent.application.service.SystemEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROOT 전용 — 시스템 이벤트 조회 API.
 */
@RestController
@RequestMapping("/admin/ops/system-events")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class SystemEventController {

    private final SystemEventService systemEventService;

    /**
     * 시스템 이벤트를 최신순으로 페이지 조회한다.
     */
    @GetMapping
    public PageResponse<SystemEventResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return PageResponse.from(systemEventService.findAll(
                PageRequest.of(page, size, Sort.by("occurredAt").descending())
        ).map(SystemEventResponse::from));
    }
}
