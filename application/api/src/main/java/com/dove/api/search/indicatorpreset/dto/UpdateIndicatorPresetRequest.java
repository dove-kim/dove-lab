package com.dove.api.search.indicatorpreset.dto;

import com.dove.screening.domain.value.IndicatorPresetItem;
import com.dove.screening.domain.value.PresetOverlay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 지표 프리셋 수정 요청.
 *
 * @param name       프리셋 이름
 * @param items      지표 항목 목록
 * @param panelOrder 패널 표시 순서
 * @param overlay    차트 오버레이 설정 (없으면 null)
 */
public record UpdateIndicatorPresetRequest(
        @NotBlank String name,
        @NotNull List<IndicatorPresetItem> items,
        List<String> panelOrder,
        PresetOverlay overlay
) {}
