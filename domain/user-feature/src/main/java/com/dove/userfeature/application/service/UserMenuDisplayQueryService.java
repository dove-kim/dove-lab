package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.UserFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.UserModuleDisplayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMenuDisplayQueryService {

    private final UserFeatureGrantQueryService grantQueryService;
    private final UserSubMenuGrantQueryService subMenuGrantQueryService;
    private final UserFeatureDisplayRepository featureDisplayRepository;
    private final UserModuleDisplayRepository moduleDisplayRepository;

    /**
     * 사용자 메뉴를 빌드한다.
     *
     * <p>활성 권한이 있는 기능만 포함하며, 숨김 여부와 순서 설정을 적용한다.
     * 각 기능에는 활성 하위 메뉴 목록이 포함된다.
     */
    public UserMenuView buildMenuForUser(Long userId) {
        Set<FeatureCode> activeFeatures = grantQueryService.findActiveFeatureCodes(userId);
        Set<SubMenuCode> activeSubMenus = subMenuGrantQueryService.findActiveSubMenuCodes(userId);

        Map<FeatureCode, UserFeatureDisplay> featureDisplayMap =
                featureDisplayRepository.findAllByUserId(userId).stream()
                        .collect(Collectors.toMap(UserFeatureDisplay::getFeatureCode, d -> d));

        Map<ModuleCode, UserModuleDisplay> moduleDisplayMap =
                moduleDisplayRepository.findAllByUserId(userId).stream()
                        .collect(Collectors.toMap(UserModuleDisplay::getModuleCode, d -> d));

        Map<ModuleCode, List<FeatureCode>> featuresByModule = activeFeatures.stream()
                .collect(Collectors.groupingBy(FeatureCode::getModule));

        List<UserMenuView.ModuleView> modules = featuresByModule.entrySet().stream()
                .map(entry -> {
                    ModuleCode module = entry.getKey();
                    UserModuleDisplay md = moduleDisplayMap.getOrDefault(
                            module, UserModuleDisplay.create(userId, module, 0));

                    List<UserMenuView.FeatureView> features = entry.getValue().stream()
                            .map(fc -> {
                                UserFeatureDisplay fd = featureDisplayMap.getOrDefault(
                                        fc, UserFeatureDisplay.create(userId, fc, 0));

                                List<UserMenuView.SubMenuView> subMenus = SubMenuCode.byFeature(fc).stream()
                                        .filter(activeSubMenus::contains)
                                        .map(UserMenuView.SubMenuView::new)
                                        .toList();

                                return new UserMenuView.FeatureView(fc, fd.getDisplayOrder(), fd.isHidden(), subMenus);
                            })
                            .sorted(Comparator.comparingInt(UserMenuView.FeatureView::displayOrder))
                            .toList();

                    return new UserMenuView.ModuleView(module, md.getDisplayOrder(), md.isHidden(), features);
                })
                .sorted(Comparator.comparingInt(UserMenuView.ModuleView::displayOrder))
                .toList();

        return new UserMenuView(modules);
    }

    public record UserMenuView(List<ModuleView> modules) {
        public record ModuleView(ModuleCode moduleCode, int displayOrder, boolean hidden, List<FeatureView> features) {}
        public record FeatureView(FeatureCode featureCode, int displayOrder, boolean hidden, List<SubMenuView> subMenus) {}
        public record SubMenuView(SubMenuCode subMenuCode) {}
    }
}
