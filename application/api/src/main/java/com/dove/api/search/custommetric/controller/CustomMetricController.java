package com.dove.api.search.custommetric.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.ops.custommetric.dto.CustomMetricSummary;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantQueryService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 검색 필터 빌더용 — 사용자가 접근 허용된 활성 커스텀 지표 목록(선택지) 조회 API.
 */
@RestController
@RequestMapping("/custom-metrics")
@RequiredArgsConstructor
@RequireCapability(Capability.CUSTOM_INDICATOR)
public class CustomMetricController {

    private final CustomMetricDefService defService;
    private final MemberCustomIndicatorGrantQueryService grantQueryService;

    /**
     * 접근 허용된 활성 커스텀 지표 요약 목록을 반환한다. ROOT는 전체, 그 외는 부여받은 지표만.
     */
    @GetMapping
    public List<CustomMetricSummary> list(@AuthenticationPrincipal AuthenticatedUser user) {
        boolean root = "ROOT".equals(user.role());
        Set<Long> granted = root ? Set.of() : grantQueryService.findGrantedMetricIds(user.memberId());
        return defService.findActive().stream()
                .filter(d -> root || granted.contains(d.getId()))
                .map(CustomMetricSummary::from)
                .toList();
    }
}
