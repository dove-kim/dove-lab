package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import com.dove.screening.domain.value.IndicatorPresetItem;
import com.dove.screening.domain.value.PresetOverlay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 지표 프리셋 변경 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IndicatorPresetCommandService {

    private final IndicatorPresetRepository indicatorPresetRepository;

    /**
     * 새 지표 프리셋을 오버레이 없이 생성한다.
     */
    public IndicatorPreset create(Long memberId, String name, List<IndicatorPresetItem> items,
                                   List<String> panelOrder) {
        return create(memberId, name, items, panelOrder, null);
    }

    /**
     * 새 지표 프리셋을 생성한다.
     */
    public IndicatorPreset create(Long memberId, String name, List<IndicatorPresetItem> items,
                                   List<String> panelOrder, PresetOverlay overlay) {
        return indicatorPresetRepository.save(
                IndicatorPreset.create(memberId, name, items, panelOrder, overlay));
    }

    /**
     * 회원이 소유한 지표 프리셋을 수정한다.
     *
     * @throws NoSuchElementException 해당 프리셋이 없을 때
     */
    public IndicatorPreset update(Long memberId, Long id, String name, List<IndicatorPresetItem> items,
                                   List<String> panelOrder) {
        return update(memberId, id, name, items, panelOrder, null);
    }

    /**
     * 회원이 소유한 지표 프리셋을 오버레이와 함께 수정한다.
     *
     * @throws NoSuchElementException 해당 프리셋이 없을 때
     */
    public IndicatorPreset update(Long memberId, Long id, String name, List<IndicatorPresetItem> items,
                                   List<String> panelOrder, PresetOverlay overlay) {
        IndicatorPreset preset = indicatorPresetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("PRESET_NOT_FOUND"));
        preset.update(name, items, panelOrder, overlay);
        return preset;
    }

    /**
     * 회원이 소유한 지표 프리셋을 삭제한다.
     *
     * @throws NoSuchElementException 해당 프리셋이 없을 때
     */
    public void delete(Long memberId, Long id) {
        IndicatorPreset preset = indicatorPresetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("PRESET_NOT_FOUND"));
        indicatorPresetRepository.delete(preset);
    }

    /**
     * 주어진 ID 순서대로 회원의 지표 프리셋 노출 순서를 재배치한다.
     */
    public void reorder(Long memberId, List<Long> orderedIds) {
        Map<Long, IndicatorPreset> presetMap = indicatorPresetRepository.findAllByMemberId(memberId)
                .stream().collect(Collectors.toMap(IndicatorPreset::getId, p -> p));
        for (int i = 0; i < orderedIds.size(); i++) {
            IndicatorPreset p = presetMap.get(orderedIds.get(i));
            if (p != null) p.updateDisplayOrder(i);
        }
    }
}
