package com.dove.stockcollection.domain.model;

import java.util.List;
import java.util.Map;

/**
 * 종목추정실적 조회 결과. 인프라(KIS)에 중립적인 원시 블록 묶음.
 *
 * @param summary    종목·애널리스트·투자의견 요약 필드 맵
 * @param income     추정손익 행별 필드 맵 목록
 * @param indicators 투자지표 행별 필드 맵 목록
 * @param periods    결산월 행별 필드 맵 목록
 */
public record AnalystEstimate(
        Map<String, Object> summary,
        List<Map<String, Object>> income,
        List<Map<String, Object>> indicators,
        List<Map<String, Object>> periods
) {
}
