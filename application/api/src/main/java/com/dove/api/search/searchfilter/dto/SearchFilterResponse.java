package com.dove.api.search.searchfilter.dto;

import com.dove.screening.domain.entity.SearchFilter;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 검색 필터 응답.
 *
 * @param id            필터 ID
 * @param name          필터 이름
 * @param dateRule      날짜 규칙
 * @param markets       대상 시장 목록
 * @param priceType     주가 유형 (RAW=비수정/ADJUSTED=수정)
 * @param exchange      지표 데이터 거래소 (KRX/NXT/INTEGRATED)
 * @param expression    검색식(원본 JSON)
 * @param pipeline      순서 단계 목록(원본 JSON 배열, null=단순 필터)
 * @param stockFilterId 종목 필터 ID
 * @param createdAt     생성 일시
 * @param updatedAt     수정 일시
 */
public record SearchFilterResponse(
        Long id,
        String name,
        String dateRule,
        List<String> markets,
        String priceType,
        String exchange,
        @JsonRawValue String expression,
        @JsonRawValue String pipeline,
        Long stockFilterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SearchFilterResponse from(SearchFilter f) {
        JsonNode root = f.getExpression() != null ? f.getExpression().root() : null;
        return new SearchFilterResponse(
                f.getId(), f.getName(),
                f.getDateRule().name(),
                f.getMarkets().stream().map(Enum::name).toList(),
                f.getPriceType().name(),
                f.getExchange().name(),
                root != null ? root.toString() : "{}",
                f.getPipeline(),
                f.getStockFilterId(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }
}
