package com.dove.api.search.stockfilter.dto;

import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 종목 필터 수정 요청.
 *
 * @param name              필터 이름
 * @param description       설명
 * @param tagConditions     태그 조건 목록
 * @param stockConditions   종목 속성 조건 목록
 * @param numericConditions 수치 조건 목록
 */
public record UpdateStockFilterRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        List<TagCondition> tagConditions,
        List<StockCondition> stockConditions,
        List<NumericCondition> numericConditions
) {}
