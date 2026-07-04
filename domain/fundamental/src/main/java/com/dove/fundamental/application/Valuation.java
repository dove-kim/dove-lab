package com.dove.fundamental.application;

/**
 * 밸류에이션 계산 결과.
 *
 * @param marketCap 시가총액(종가×상장주식수)
 * @param per       시총/당기순이익(TTM, 지배주주) — 분모 없음·0이면 null
 * @param pbr       시총/자본(지배주주지분)
 * @param psr       시총/매출액(TTM)
 * @param gpa       매출총이익(TTM)/자산총계
 */
public record Valuation(Long marketCap, Double per, Double pbr, Double psr, Double gpa) {
}
