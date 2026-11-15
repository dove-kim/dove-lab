package com.dove.api.ops.fx.service;

import com.dove.portfolio.application.service.FxRateCoverage;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 환율 이력 운영 조합 — 보유 현황·최근 고시 조회와 수동 채우기를 묶는다.
 */
@Service
@RequiredArgsConstructor
public class FxRateOpsService {

    private final PortfolioFxRateService fxRateService;
    private final PortfolioTransactionService transactionService;

    /**
     * 통화별 환율 이력 보유 현황을 반환한다.
     */
    public List<FxRateCoverage> coverage() {
        return fxRateService.coverage();
    }

    /**
     * 최근 고시일의 환율 이력을 반환한다.
     */
    public List<PortfolioFxRate> recentRates(int days) {
        return fxRateService.recentRates(days);
    }

    /**
     * 백필 시작 기준일(가장 오래된 거래일)을 반환한다. 거래가 없으면 오늘.
     */
    public LocalDate backfillStartDate() {
        return transactionService.oldestTradeDate().orElseGet(LocalDate::now);
    }

    /**
     * 빈 구간의 환율을 지금 채운다.
     *
     * @return 새로 저장된 행 수
     */
    public int refresh() {
        return fxRateService.refreshFrom(backfillStartDate());
    }
}
