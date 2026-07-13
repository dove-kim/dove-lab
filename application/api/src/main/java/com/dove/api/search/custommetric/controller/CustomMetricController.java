package com.dove.api.search.custommetric.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.ops.custommetric.dto.CustomMetricSummary;
import com.dove.api.ops.custommetric.dto.MetricPoint;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.custommetric.domain.entity.CustomMetricDef;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantQueryService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 검색 필터 빌더·차트 오버레이용 — 사용자가 접근 허용된 커스텀 지표 목록·시계열 조회 API.
 */
@RestController
@RequestMapping("/custom-metrics")
@RequiredArgsConstructor
@RequireCapability(Capability.CUSTOM_INDICATOR)
public class CustomMetricController {

    private final CustomMetricDefService defService;
    private final CustomMetricDailyService dailyService;
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

    /**
     * 지표의 저장 시계열을 거래일 범위(양끝 포함, 오름차순)로 반환한다. from/to는 선택(무제한).
     * 활성 지표가 없으면 빈 목록.
     *
     * @throws ResponseStatusException 지표 접근이 부여되지 않은 경우(403 CUSTOM_METRIC_NOT_GRANTED)
     */
    @GetMapping("/{id}/series")
    public List<MetricPoint> series(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean root = "ROOT".equals(user.role());
        if (!root && !grantQueryService.hasGrant(user.memberId(), id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CUSTOM_METRIC_NOT_GRANTED");
        }
        return defService.findById(id)
                .filter(CustomMetricDef::isActive)
                .map(def -> dailyService.findByMetricAndDateRange(id, from, to).stream()
                        .map(row -> new MetricPoint(row.getId().getTradeDate().toString(), row.getValue()))
                        .toList())
                .orElse(List.of());
    }
}
