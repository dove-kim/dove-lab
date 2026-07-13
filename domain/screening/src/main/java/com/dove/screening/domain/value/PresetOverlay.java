package com.dove.screening.domain.value;

import java.util.List;

/**
 * 지표 프리셋의 차트 오버레이 설정 (모델 시그널·커스텀 지표).
 *
 * @param signalModelId   시그널 모델 ID (없으면 null)
 * @param signalThreshold 시그널 임계값 (0~1)
 * @param seriesMetricIds 시장 시계열 지표 ID 목록
 */
public record PresetOverlay(
        Long signalModelId,
        Double signalThreshold,
        List<Long> seriesMetricIds
) {}
