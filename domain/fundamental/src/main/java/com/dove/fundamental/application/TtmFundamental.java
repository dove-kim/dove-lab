package com.dove.fundamental.application;

/**
 * 밸류에이션 입력 스냅샷 — flow는 TTM(최근 4분기 합산), stock은 최신 보고서 시점값.
 * 연결(CFS) 기준 PER·PBR은 지배주주 몫으로 계산하므로 netIncome·equity는 지배주주값 우선(없으면 전체)을 담는다.
 *
 * @param revenue       매출액(TTM, 전체)
 * @param grossProfit   매출총이익(TTM, 전체)
 * @param netIncome     당기순이익(TTM, 지배주주 우선)
 * @param equity        자본(최신 시점, 지배주주지분 우선)
 * @param totalAsset    자산총계(최신 시점, 전체)
 * @param latestRceptNo 시점(최신) 보고서 접수번호(감사추적)
 */
public record TtmFundamental(
        Long revenue,
        Long grossProfit,
        Long netIncome,
        Long equity,
        Long totalAsset,
        String latestRceptNo) {
}
