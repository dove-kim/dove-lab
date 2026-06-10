package com.dove.userfeature.application.service;

import com.dove.auth.domain.enums.MemberRole;
import com.dove.userfeature.application.dto.MemberMenuView;
import com.dove.userfeature.application.dto.ModuleView;
import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.MemberFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.MemberModuleDisplayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberMenuDisplayQueryService")
class MemberMenuDisplayQueryServiceTest {

    @Mock MemberFeatureGrantQueryService grantQueryService;
    @Mock MemberSubMenuGrantQueryService subMenuGrantQueryService;
    @Mock MemberFeatureDisplayRepository featureDisplayRepository;
    @Mock MemberModuleDisplayRepository moduleDisplayRepository;
    @InjectMocks MemberMenuDisplayQueryService service;

    private static final Long MEMBER_ID = 1L;

    @Nested
    @DisplayName("buildMenuForMember")
    class BuildMenuForMember {

        @Test
        @DisplayName("shouldIncludeOnlyActiveFeaturesWithDisplayOrder")
        void shouldIncludeOnlyActiveFeaturesWithDisplayOrder() {
            given(grantQueryService.findActiveFeatureCodes(MEMBER_ID))
                    .willReturn(Set.of(FeatureCode.STOCK_SEARCH, FeatureCode.STOCK_LEDGER));
            given(subMenuGrantQueryService.findActiveSubMenuCodes(MEMBER_ID)).willReturn(Set.of());
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
                    MemberFeatureDisplay.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, 1),
                    MemberFeatureDisplay.create(MEMBER_ID, FeatureCode.STOCK_LEDGER, 0)
            ));
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
                    MemberModuleDisplay.create(MEMBER_ID, ModuleCode.STOCK, 0)
            ));

            MemberMenuView menu = service.buildMenuForMember(MEMBER_ID, MemberRole.USER);

            assertThat(menu.modules()).hasSize(1);
            ModuleView stockModule = menu.modules().get(0);
            assertThat(stockModule.moduleCode()).isEqualTo(ModuleCode.STOCK);
            assertThat(stockModule.displayOrder()).isEqualTo(0);
            // STOCK_LEDGER(order=0)이 먼저, STOCK_SEARCH(order=1)가 나중
            assertThat(stockModule.features().get(0).featureCode()).isEqualTo(FeatureCode.STOCK_LEDGER);
            assertThat(stockModule.features().get(1).featureCode()).isEqualTo(FeatureCode.STOCK_SEARCH);
        }

        @Test
        @DisplayName("shouldReturnEmptyMenuWhenNoActiveFeatures")
        void shouldReturnEmptyMenuWhenNoActiveFeatures() {
            given(grantQueryService.findActiveFeatureCodes(MEMBER_ID)).willReturn(Set.of());
            given(subMenuGrantQueryService.findActiveSubMenuCodes(MEMBER_ID)).willReturn(Set.of());
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

            MemberMenuView menu = service.buildMenuForMember(MEMBER_ID, MemberRole.USER);

            assertThat(menu.modules()).isEmpty();
        }

        @Test
        @DisplayName("shouldApplyDefaultDisplaySettingsWhenAbsent")
        void shouldApplyDefaultDisplaySettingsWhenAbsent() {
            given(grantQueryService.findActiveFeatureCodes(MEMBER_ID))
                    .willReturn(Set.of(FeatureCode.BUDGET));
            given(subMenuGrantQueryService.findActiveSubMenuCodes(MEMBER_ID)).willReturn(Set.of());
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

            MemberMenuView menu = service.buildMenuForMember(MEMBER_ID, MemberRole.USER);

            ModuleView module = menu.modules().get(0);
            assertThat(module.hidden()).isFalse();
            assertThat(module.features().get(0).hidden()).isFalse();
        }

        @Test
        @DisplayName("shouldReflectHiddenSettingsForModuleAndFeature")
        void shouldReflectHiddenSettingsForModuleAndFeature() {
            given(grantQueryService.findActiveFeatureCodes(MEMBER_ID))
                    .willReturn(Set.of(FeatureCode.STOCK_SEARCH));
            given(subMenuGrantQueryService.findActiveSubMenuCodes(MEMBER_ID)).willReturn(Set.of());
            MemberFeatureDisplay hidden = MemberFeatureDisplay.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, 0);
            hidden.setHidden(true);
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(hidden));
            MemberModuleDisplay hiddenModule = MemberModuleDisplay.create(MEMBER_ID, ModuleCode.STOCK, 0);
            hiddenModule.setHidden(true);
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(hiddenModule));

            MemberMenuView menu = service.buildMenuForMember(MEMBER_ID, MemberRole.USER);

            assertThat(menu.modules().get(0).hidden()).isTrue();
            assertThat(menu.modules().get(0).features().get(0).hidden()).isTrue();
        }
    }
}
