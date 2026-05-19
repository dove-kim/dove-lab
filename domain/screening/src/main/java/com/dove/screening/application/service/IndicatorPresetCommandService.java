package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IndicatorPresetCommandService {

    private final IndicatorPresetRepository indicatorPresetRepository;

    public IndicatorPreset create(Long memberId, String name, String items, String panelOrder) {
        return indicatorPresetRepository.save(
                IndicatorPreset.create(memberId, name, items, panelOrder));
    }

    public IndicatorPreset update(Long memberId, Long id, String name, String items, String panelOrder) {
        IndicatorPreset preset = indicatorPresetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("PRESET_NOT_FOUND"));
        preset.update(name, items, panelOrder);
        return preset;
    }

    public void delete(Long memberId, Long id) {
        IndicatorPreset preset = indicatorPresetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("PRESET_NOT_FOUND"));
        indicatorPresetRepository.delete(preset);
    }

    public void reorder(Long memberId, List<Long> orderedIds) {
        Map<Long, IndicatorPreset> presetMap = indicatorPresetRepository.findAllByMemberId(memberId)
                .stream().collect(Collectors.toMap(IndicatorPreset::getId, p -> p));
        for (int i = 0; i < orderedIds.size(); i++) {
            IndicatorPreset p = presetMap.get(orderedIds.get(i));
            if (p != null) p.updateDisplayOrder(i);
        }
    }
}
