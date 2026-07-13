package com.dove.api.search.searchfilter.dto;

import com.dove.screening.domain.entity.SearchFilter;
import com.dove.market.domain.enums.MarketType;

import java.time.LocalDate;
import java.util.List;

/**
 * 검색 필터 실행 응답.
 *
 * @param filterId        필터 ID
 * @param filterName      필터 이름
 * @param evaluationDate  평가 기준일
 * @param dateRule        날짜 규칙
 * @param markets         대상 시장 목록
 * @param totalCandidates 후보 종목 수
 * @param matchCount      매칭 종목 수
 * @param results         매칭 결과 목록
 */
public record ExecuteFilterResponse(
        Long filterId,
        String filterName,
        LocalDate evaluationDate,
        String dateRule,
        List<String> markets,
        int totalCandidates,
        int matchCount,
        List<StockMatchResult> results
) {
    public static ExecuteFilterResponse from(SearchFilter filter, FilterExecutionResult result) {
        List<StockMatchResult> results = result.matches().stream()
                .map(m -> new StockMatchResult(m.ticker(), m.name(), m.market(), m.openPrice(),
                        m.highPrice(), m.lowPrice(), m.closePrice(), m.volume(), m.prevClose(), m.marketCap(),
                        m.modelScore()))
                .toList();
        return new ExecuteFilterResponse(
                filter.getId(), filter.getName(), result.evalDate(),
                filter.getDateRule().name(),
                filter.getMarkets().stream().map(MarketType::name).toList(),
                result.totalCandidates(), results.size(), results);
    }
}
