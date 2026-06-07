package com.dove.api.search.stock.dto;

import com.dove.stockcollection.domain.model.AnalystEstimate;

import java.util.List;
import java.util.Map;

/**
 * 종목추정실적 응답.
 *
 * @param available 데이터 존재 여부
 * @param analyst 담당 애널리스트
 * @param opinion 투자의견
 * @param periods 결산월(컬럼)
 * @param income 추정손익 행별 값 배열
 * @param indicators 투자지표 행별 값 배열
 */
public record EstimateResponse(
        boolean available,
        String analyst,
        String opinion,
        List<String> periods,
        List<List<String>> income,
        List<List<String>> indicators
) {
    private static final EstimateResponse EMPTY =
            new EstimateResponse(false, null, null, List.of(), List.of(), List.of());

    public static EstimateResponse from(AnalystEstimate e) {
        if (e == null || e.summary() == null || e.periods() == null || e.periods().isEmpty()) {
            return EMPTY;
        }
        List<String> periods = e.periods().stream().map(m -> str(m, "dt")).toList();
        return new EstimateResponse(
                true,
                str(e.summary(), "name1"),
                str(e.summary(), "rcmd_name"),
                periods,
                rows(e.income()),
                rows(e.indicators()));
    }

    public static EstimateResponse empty() {
        return EMPTY;
    }

    private static List<List<String>> rows(List<Map<String, Object>> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream()
                .map(m -> List.of(str(m, "data1"), str(m, "data2"), str(m, "data3"), str(m, "data4"), str(m, "data5")))
                .toList();
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
