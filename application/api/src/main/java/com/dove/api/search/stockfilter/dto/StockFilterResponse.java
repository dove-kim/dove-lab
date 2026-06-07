package com.dove.api.search.stockfilter.dto;

import com.dove.screening.domain.entity.StockFilter;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 종목 필터 응답.
 *
 * @param id                필터 ID
 * @param scope             범위(SYSTEM/MEMBER)
 * @param name              필터 이름
 * @param description       설명
 * @param tagConditions     태그 조건 목록
 * @param stockConditions   종목 속성 조건 목록
 * @param numericConditions 수치 조건 목록
 * @param enabled           활성화 여부
 * @param displayOrder      표시 순서
 * @param createdBy         생성자
 * @param createdAt         생성 일시
 * @param updatedAt         수정 일시
 * @param updatedBy         수정자
 */
public record StockFilterResponse(
        Long id,
        String scope,
        String name,
        String description,
        List<TagCondition> tagConditions,
        List<StockCondition> stockConditions,
        List<NumericCondition> numericConditions,
        boolean enabled,
        int displayOrder,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {
    /**
     * 종목 필터 엔티티로부터 응답 객체를 생성한다.
     */
    public static StockFilterResponse from(StockFilter f) {
        return new StockFilterResponse(
                f.getId(),
                f.isSystem() ? "SYSTEM" : "MEMBER",
                f.getName(),
                f.getDescription(),
                f.getTagConditions(),
                f.getStockConditions(),
                f.getNumericConditions(),
                f.isEnabled(),
                f.getDisplayOrder(),
                f.getCreatedBy(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                f.getUpdatedBy()
        );
    }
}
