package com.dove.api.member.custommetricgrant.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.member.custommetricgrant.dto.UpdateUserCustomIndicatorRequest;
import com.dove.api.ops.custommetric.dto.CustomMetricSummary;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantCommandService;
import com.dove.userfeature.application.service.MemberCustomIndicatorGrantQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 사용자별 커스텀 지표 접근 부여 관리 API (ADMIN 이상). 지표 계산식(정의)은 노출하지 않는다.
 */
@RestController
@RequestMapping("/admin/custom-metric-grants")
@RequiredArgsConstructor
@RequireRole(Role.ADMIN)
public class MemberCustomIndicatorGrantController {

    private final CustomMetricDefService defService;
    private final MemberCustomIndicatorGrantCommandService commandService;
    private final MemberCustomIndicatorGrantQueryService queryService;

    /**
     * 부여 대상이 되는 활성 지표 요약 목록을 반환한다(이름·모양만, 계산식 제외).
     */
    @GetMapping("/metrics")
    public List<CustomMetricSummary> grantableMetrics() {
        return defService.findActive().stream().map(CustomMetricSummary::from).toList();
    }

    /**
     * 회원에게 부여된 지표 ID 집합을 조회한다.
     */
    @GetMapping("/users/{userId}")
    public Set<Long> getGrants(@PathVariable("userId") Long memberId) {
        return queryService.findGrantedMetricIds(memberId);
    }

    /**
     * 회원에게 지표 접근을 부여하거나 회수한다.
     */
    @PatchMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGrant(
            @PathVariable("userId") Long memberId,
            @RequestBody @Valid UpdateUserCustomIndicatorRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        switch (request.action()) {
            case GRANT -> commandService.grant(memberId, request.metricId(), admin.memberId());
            case REVOKE -> commandService.revoke(memberId, request.metricId());
        }
    }
}
