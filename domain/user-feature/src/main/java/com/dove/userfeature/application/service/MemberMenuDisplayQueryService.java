package com.dove.userfeature.application.service;

import com.dove.auth.domain.enums.MemberRole;
import com.dove.userfeature.application.dto.FeatureView;
import com.dove.userfeature.application.dto.MemberMenuView;
import com.dove.userfeature.application.dto.ModuleView;
import com.dove.userfeature.application.dto.SubMenuView;
import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.MemberFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.MemberModuleDisplayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 메뉴 트리 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberMenuDisplayQueryService {

    private final MemberFeatureGrantQueryService grantQueryService;
    private final MemberSubMenuGrantQueryService subMenuGrantQueryService;
    private final MemberFeatureDisplayRepository featureDisplayRepository;
    private final MemberModuleDisplayRepository moduleDisplayRepository;

    /**
     * 사용자의 메뉴 트리를 반환한다.
     *
     * @param memberId 회원 식별자
     * @param role     회원 권한 (ROOT이면 모든 기능을 자동 부여)
     */
    public MemberMenuView buildMenuForMember(Long memberId, MemberRole role) {
        Set<FeatureCode> activeFeatures = role == MemberRole.ROOT
                ? Arrays.stream(FeatureCode.values()).collect(Collectors.toSet())
                : grantQueryService.findActiveFeatureCodes(memberId);
        Set<SubMenuCode> activeSubMenus = subMenuGrantQueryService.findActiveSubMenuCodes(memberId);

        Map<FeatureCode, MemberFeatureDisplay> featureDisplayMap =
                featureDisplayRepository.findAllByMemberId(memberId).stream()
                        .collect(Collectors.toMap(MemberFeatureDisplay::getFeatureCode, d -> d));

        Map<ModuleCode, MemberModuleDisplay> moduleDisplayMap =
                moduleDisplayRepository.findAllByMemberId(memberId).stream()
                        .collect(Collectors.toMap(MemberModuleDisplay::getModuleCode, d -> d));

        Map<ModuleCode, List<FeatureCode>> featuresByModule = activeFeatures.stream()
                .collect(Collectors.groupingBy(FeatureCode::getModule));

        List<ModuleView> modules = featuresByModule.entrySet().stream()
                .map(entry -> {
                    ModuleCode module = entry.getKey();
                    MemberModuleDisplay md = moduleDisplayMap.getOrDefault(
                            module, MemberModuleDisplay.create(memberId, module, 0));

                    List<FeatureView> features = entry.getValue().stream()
                            .map(fc -> {
                                MemberFeatureDisplay fd = featureDisplayMap.getOrDefault(
                                        fc, MemberFeatureDisplay.create(memberId, fc, 0));

                                List<SubMenuView> subMenus = SubMenuCode.byFeature(fc).stream()
                                        .filter(activeSubMenus::contains)
                                        .map(SubMenuView::new)
                                        .toList();

                                return new FeatureView(fc, fd.getDisplayOrder(), fd.isHidden(), subMenus);
                            })
                            .sorted(Comparator.comparingInt(FeatureView::displayOrder))
                            .toList();

                    return new ModuleView(module, md.getDisplayOrder(), md.isHidden(), features);
                })
                .sorted(Comparator.comparingInt(ModuleView::displayOrder))
                .toList();

        return new MemberMenuView(modules);
    }
}
