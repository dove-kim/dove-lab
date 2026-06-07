package com.dove.api.search.searchfilter.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 검색 필터 실행 결과 읽기 모델.
 */
public record FilterExecutionResult(
        LocalDate evalDate,
        int totalCandidates,
        List<MatchedStock> matches
) {
}
