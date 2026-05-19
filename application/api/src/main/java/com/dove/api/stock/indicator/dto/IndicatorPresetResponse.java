package com.dove.api.stock.indicator.dto;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record IndicatorPresetResponse(
        Long id,
        String name,
        @JsonRawValue String items,
        List<String> panelOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IndicatorPresetResponse from(IndicatorPreset preset) {
        List<String> panels = preset.getPanelOrder() != null && !preset.getPanelOrder().isBlank()
                ? Arrays.stream(preset.getPanelOrder().split(",")).map(String::trim).toList()
                : List.of();
        return new IndicatorPresetResponse(
                preset.getId(),
                preset.getName(),
                preset.getItems(),
                panels,
                preset.getCreatedAt(),
                preset.getUpdatedAt()
        );
    }
}
