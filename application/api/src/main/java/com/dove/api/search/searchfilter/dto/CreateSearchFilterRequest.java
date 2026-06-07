package com.dove.api.search.searchfilter.dto;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.enums.DateRule;
import com.dove.stock.domain.enums.PriceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 검색 필터 생성 요청.
 *
 * @param name          필터 이름
 * @param dateRule      날짜 규칙
 * @param markets       대상 시장 목록
 * @param priceType     주가 유형 (RAW=비수정/ADJUSTED=수정, 미지정 시 RAW)
 * @param expression    검색식
 * @param stockFilterId 종목 필터 ID
 */
public record CreateSearchFilterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DateRule dateRule,
        @NotEmpty List<MarketType> markets,
        PriceType priceType,
        @NotBlank String expression,
        Long stockFilterId
) {}
