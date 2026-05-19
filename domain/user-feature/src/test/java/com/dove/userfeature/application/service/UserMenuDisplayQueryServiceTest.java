package com.dove.userfeature.application.service;

import com.dove.userfeature.application.service.UserMenuDisplayQueryService.UserMenuView;
import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.UserFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.UserModuleDisplayRepository;
import org.junit.jupiter.api.DisplayName;
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
class UserMenuDisplayQueryServiceTest {

    @Mock UserFeatureGrantQueryService grantQueryService;
    @Mock UserSubMenuGrantQueryService subMenuGrantQueryService;
    @Mock UserFeatureDisplayRepository featureDisplayRepository;
    @Mock UserModuleDisplayRepository moduleDisplayRepository;
    @InjectMocks UserMenuDisplayQueryService service;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("buildMenuForUser — 활성 기능만 포함, 모듈·기능 순서 적용")
    void shouldBuildMenuWithActiveFeatures() {
        given(grantQueryService.findActiveFeatureCodes(USER_ID))
                .willReturn(Set.of(FeatureCode.STOCK_SEARCH, FeatureCode.STOCK_LEDGER));
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(
                UserFeatureDisplay.create(USER_ID, FeatureCode.STOCK_SEARCH, 1),
                UserFeatureDisplay.create(USER_ID, FeatureCode.STOCK_LEDGER, 0)
        ));
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(
                UserModuleDisplay.create(USER_ID, ModuleCode.STOCK, 0)
        ));

        UserMenuView menu = service.buildMenuForUser(USER_ID);

        assertThat(menu.modules()).hasSize(1);
        UserMenuView.ModuleView stockModule = menu.modules().get(0);
        assertThat(stockModule.moduleCode()).isEqualTo(ModuleCode.STOCK);
        assertThat(stockModule.displayOrder()).isEqualTo(0);
        // STOCK_LEDGER(order=0)이 먼저, STOCK_SEARCH(order=1)가 나중
        assertThat(stockModule.features().get(0).featureCode()).isEqualTo(FeatureCode.STOCK_LEDGER);
        assertThat(stockModule.features().get(1).featureCode()).isEqualTo(FeatureCode.STOCK_SEARCH);
    }

    @Test
    @DisplayName("buildMenuForUser — 활성 기능 없으면 빈 메뉴")
    void shouldReturnEmptyMenuWhenNoActiveFeatures() {
        given(grantQueryService.findActiveFeatureCodes(USER_ID)).willReturn(Set.of());
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());

        UserMenuView menu = service.buildMenuForUser(USER_ID);

        assertThat(menu.modules()).isEmpty();
    }

    @Test
    @DisplayName("buildMenuForUser — 표시 설정 없으면 기본값(order=0, hidden=false) 적용")
    void shouldApplyDefaultDisplaySettingsWhenAbsent() {
        given(grantQueryService.findActiveFeatureCodes(USER_ID))
                .willReturn(Set.of(FeatureCode.BUDGET));
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());

        UserMenuView menu = service.buildMenuForUser(USER_ID);

        UserMenuView.ModuleView module = menu.modules().get(0);
        assertThat(module.hidden()).isFalse();
        assertThat(module.features().get(0).hidden()).isFalse();
    }

    @Test
    @DisplayName("buildMenuForUser — 숨김 설정 반영")
    void shouldReflectHiddenSettings() {
        given(grantQueryService.findActiveFeatureCodes(USER_ID))
                .willReturn(Set.of(FeatureCode.STOCK_SEARCH));
        UserFeatureDisplay hidden = UserFeatureDisplay.create(USER_ID, FeatureCode.STOCK_SEARCH, 0);
        hidden.setHidden(true);
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(hidden));
        UserModuleDisplay hiddenModule = UserModuleDisplay.create(USER_ID, ModuleCode.STOCK, 0);
        hiddenModule.setHidden(true);
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(hiddenModule));

        UserMenuView menu = service.buildMenuForUser(USER_ID);

        assertThat(menu.modules().get(0).hidden()).isTrue();
        assertThat(menu.modules().get(0).features().get(0).hidden()).isTrue();
    }
}
