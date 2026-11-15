package com.dove.api.ops.fx.dto;

import java.util.List;

/**
 * 환율 이력 운영 화면 응답.
 *
 * @param backfillStartDate 이력이 없는 통화의 백필 시작 기준일(ISO, 가장 오래된 거래일)
 * @param coverages         통화별 보유 현황
 * @param recent            최근 고시 환율(고시일 내림차순)
 */
public record FxRateOpsResponse(
        String backfillStartDate,
        List<FxRateCoverageResponse> coverages,
        List<FxRateRowResponse> recent
) {}
