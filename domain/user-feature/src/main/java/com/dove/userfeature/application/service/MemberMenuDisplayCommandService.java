package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.MemberFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.MemberModuleDisplayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 사용자 메뉴 표시 설정(순서·숨김) 변경 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberMenuDisplayCommandService {

    private final MemberFeatureDisplayRepository featureDisplayRepository;
    private final MemberModuleDisplayRepository moduleDisplayRepository;

    /**
     * 모듈 순서를 변경한다.
     */
    public void reorderModules(Long memberId, List<ModuleCode> orderedModules) {
        Map<ModuleCode, MemberModuleDisplay> map = moduleDisplayRepository.findAllByMemberId(memberId)
                .stream().collect(Collectors.toMap(MemberModuleDisplay::getModuleCode, d -> d));
        for (int i = 0; i < orderedModules.size(); i++) {
            MemberModuleDisplay display = map.get(orderedModules.get(i));
            if (display != null) display.updateDisplayOrder(i);
        }
    }

    /**
     * 모듈 내 기능 순서를 변경한다.
     */
    public void reorderFeatures(Long memberId, ModuleCode module, List<FeatureCode> orderedFeatures) {
        Map<FeatureCode, MemberFeatureDisplay> map = featureDisplayRepository.findAllByMemberId(memberId)
                .stream()
                .filter(d -> d.getFeatureCode().getModule() == module)
                .collect(Collectors.toMap(MemberFeatureDisplay::getFeatureCode, d -> d));
        for (int i = 0; i < orderedFeatures.size(); i++) {
            MemberFeatureDisplay display = map.get(orderedFeatures.get(i));
            if (display != null) display.updateDisplayOrder(i);
        }
    }

    /**
     * 기능 숨김 상태를 변경한다.
     */
    public void setFeatureHidden(Long memberId, FeatureCode featureCode, boolean hidden) {
        featureDisplayRepository.findByMemberIdAndFeatureCode(memberId, featureCode)
                .orElseThrow(() -> new NoSuchElementException("FEATURE_DISPLAY_NOT_FOUND"))
                .setHidden(hidden);
    }

    /**
     * 모듈 숨김 상태를 변경한다.
     */
    public void setModuleHidden(Long memberId, ModuleCode moduleCode, boolean hidden) {
        moduleDisplayRepository.findByMemberIdAndModuleCode(memberId, moduleCode)
                .orElseThrow(() -> new NoSuchElementException("MODULE_DISPLAY_NOT_FOUND"))
                .setHidden(hidden);
    }
}
