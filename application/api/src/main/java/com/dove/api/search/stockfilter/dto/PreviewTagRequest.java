package com.dove.api.search.stockfilter.dto;

import com.dove.market.domain.enums.MarketType;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.TagCondition;

import java.util.List;

/**
 * 태그·수치 조건 미리보기 요청.
 */
public record PreviewTagRequest(
        List<TagCondition> tagConditions,
        List<NumericCondition> numericConditions,
        List<MarketType> markets
) {
}
