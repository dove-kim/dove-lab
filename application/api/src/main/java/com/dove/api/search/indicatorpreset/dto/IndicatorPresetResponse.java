package com.dove.api.search.indicatorpreset.dto;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.value.IndicatorPresetItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 지표 프리셋 응답.
 *
 * @param id         프리셋 ID
 * @param name       프리셋 이름
 * @param items      지표 항목 목록
 * @param panelOrder 패널 표시 순서
 * @param createdAt  생성 일시
 * @param updatedAt  수정 일시
 */
public record IndicatorPresetResponse(
        Long id,
        String name,
        List<IndicatorPresetItem> items,
        List<String> panelOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IndicatorPresetResponse from(IndicatorPreset preset) {
        return new IndicatorPresetResponse(
                preset.getId(),
                preset.getName(),
                preset.getItems() != null ? preset.getItems() : List.of(),
                preset.getPanelOrder() != null ? preset.getPanelOrder() : List.of(),
                preset.getCreatedAt(),
                preset.getUpdatedAt()
        );
    }
}
