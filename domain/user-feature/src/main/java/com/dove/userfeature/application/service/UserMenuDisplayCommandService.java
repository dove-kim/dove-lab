package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.UserFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.UserModuleDisplayRepository;
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
public class UserMenuDisplayCommandService {

    private final UserFeatureDisplayRepository featureDisplayRepository;
    private final UserModuleDisplayRepository moduleDisplayRepository;

    /** 모듈 순서 변경. orderedModules 인덱스 순서대로 displayOrder 갱신. */
    public void reorderModules(Long userId, List<ModuleCode> orderedModules) {
        Map<ModuleCode, UserModuleDisplay> map = moduleDisplayRepository.findAllByUserId(userId)
                .stream().collect(Collectors.toMap(UserModuleDisplay::getModuleCode, d -> d));
        for (int i = 0; i < orderedModules.size(); i++) {
            UserModuleDisplay display = map.get(orderedModules.get(i));
            if (display != null) display.updateDisplayOrder(i);
        }
    }

    /** 모듈 내 기능 순서 변경. */
    public void reorderFeatures(Long userId, ModuleCode module, List<FeatureCode> orderedFeatures) {
        Map<FeatureCode, UserFeatureDisplay> map = featureDisplayRepository.findAllByUserId(userId)
                .stream()
                .filter(d -> d.getFeatureCode().getModule() == module)
                .collect(Collectors.toMap(UserFeatureDisplay::getFeatureCode, d -> d));
        for (int i = 0; i < orderedFeatures.size(); i++) {
            UserFeatureDisplay display = map.get(orderedFeatures.get(i));
            if (display != null) display.updateDisplayOrder(i);
        }
    }

    /** 기능 숨김 상태 변경. */
    public void setFeatureHidden(Long userId, FeatureCode featureCode, boolean hidden) {
        featureDisplayRepository.findByUserIdAndFeatureCode(userId, featureCode)
                .orElseThrow(() -> new NoSuchElementException("FEATURE_DISPLAY_NOT_FOUND"))
                .setHidden(hidden);
    }

    /** 모듈 숨김 상태 변경. */
    public void setModuleHidden(Long userId, ModuleCode moduleCode, boolean hidden) {
        moduleDisplayRepository.findByUserIdAndModuleCode(userId, moduleCode)
                .orElseThrow(() -> new NoSuchElementException("MODULE_DISPLAY_NOT_FOUND"))
                .setHidden(hidden);
    }
}
