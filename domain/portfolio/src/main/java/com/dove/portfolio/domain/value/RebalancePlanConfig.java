package com.dove.portfolio.domain.value;

import java.util.List;

/**
 * 리밸런싱 계획 설정 — 종목 배분·전략 현금·슬롯 수·유동성 참여율의 묶음.
 *
 * @param slots     슬롯 수(신규 매수 시 총자산 분할 수)
 * @param partRate  유동성 참여율(%) — 종목별 상한 = 참여율 × 거래대금
 * @param positions 종목 목표 배분 항목
 * @param cash      전략 현금 라인
 */
public record RebalancePlanConfig(int slots, double partRate, List<RebalancePlanEntry> positions,
                                  List<RebalancePlanCash> cash) {
    public RebalancePlanConfig {
        positions = positions == null ? List.of() : positions;
        cash = cash == null ? List.of() : cash;
    }
}
