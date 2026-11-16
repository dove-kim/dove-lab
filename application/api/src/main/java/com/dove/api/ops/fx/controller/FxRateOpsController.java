package com.dove.api.ops.fx.controller;

import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.ops.fx.dto.FxRateCoverageResponse;
import com.dove.api.ops.fx.dto.FxRateOpsResponse;
import com.dove.api.ops.fx.dto.FxRateRefreshResponse;
import com.dove.api.ops.fx.dto.FxRateRowResponse;
import com.dove.api.ops.fx.service.FxRateOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 환율 이력 운영 API.
 */
@RestController
@RequestMapping("/admin/ops/fx")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class FxRateOpsController {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 180;

    private final FxRateOpsService service;

    /**
     * 통화별 보유 현황과 최근 고시 환율을 반환한다.
     *
     * @param days 최근 몇 고시일을 볼지(1 이상 180 이하)
     */
    @GetMapping
    public FxRateOpsResponse get(@RequestParam(defaultValue = "" + DEFAULT_DAYS) int days) {
        int capped = Math.clamp(days, 1, MAX_DAYS);
        return new FxRateOpsResponse(
                service.backfillStartDate().toString(),
                service.coverage().stream().map(FxRateCoverageResponse::from).toList(),
                service.recentRates(capped).stream().map(FxRateRowResponse::from).toList());
    }

    /**
     * 빈 구간의 환율과 해외 종가를 지금 채운다.
     */
    @PostMapping("/refresh")
    public FxRateRefreshResponse refresh() {
        return new FxRateRefreshResponse(service.refreshRates(), service.refreshQuotes());
    }
}
