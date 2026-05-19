package com.dove.userfeature.application.service;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserMenuDisplayCommandServiceTest {

    @Mock UserFeatureDisplayRepository featureDisplayRepository;
    @Mock UserModuleDisplayRepository moduleDisplayRepository;
    @InjectMocks UserMenuDisplayCommandService service;

    private static final Long USER_ID = 1L;

    private UserModuleDisplay moduleDisplay(ModuleCode code, int order) {
        UserModuleDisplay d = UserModuleDisplay.create(USER_ID, code, order);
        ReflectionTestUtils.setField(d, "id", (long) order + 1);
        return d;
    }

    private UserFeatureDisplay featureDisplay(FeatureCode code, int order) {
        return UserFeatureDisplay.create(USER_ID, code, order);
    }

    // ─── reorderModules ───────────────────────────────────────────────────

    @Test
    @DisplayName("reorderModules — 전달 순서대로 displayOrder 갱신")
    void shouldReorderModules() {
        UserModuleDisplay stock = moduleDisplay(ModuleCode.STOCK, 1);
        UserModuleDisplay budget = moduleDisplay(ModuleCode.BUDGET, 0);
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(stock, budget));

        service.reorderModules(USER_ID, List.of(ModuleCode.STOCK, ModuleCode.BUDGET));

        assertThat(stock.getDisplayOrder()).isEqualTo(0);
        assertThat(budget.getDisplayOrder()).isEqualTo(1);
    }

    // ─── reorderFeatures ──────────────────────────────────────────────────

    @Test
    @DisplayName("reorderFeatures — 모듈 내 기능 순서 갱신")
    void shouldReorderFeaturesWithinModule() {
        UserFeatureDisplay search = featureDisplay(FeatureCode.STOCK_SEARCH, 1);
        UserFeatureDisplay ledger = featureDisplay(FeatureCode.STOCK_LEDGER, 0);
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(search, ledger));

        service.reorderFeatures(USER_ID, ModuleCode.STOCK,
                List.of(FeatureCode.STOCK_SEARCH, FeatureCode.STOCK_LEDGER));

        assertThat(search.getDisplayOrder()).isEqualTo(0);
        assertThat(ledger.getDisplayOrder()).isEqualTo(1);
    }

    // ─── setFeatureHidden ─────────────────────────────────────────────────

    @Test
    @DisplayName("setFeatureHidden — hidden=true 설정")
    void shouldHideFeature() {
        UserFeatureDisplay display = featureDisplay(FeatureCode.STOCK_SEARCH, 0);
        given(featureDisplayRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(display));

        service.setFeatureHidden(USER_ID, FeatureCode.STOCK_SEARCH, true);

        assertThat(display.isHidden()).isTrue();
    }

    @Test
    @DisplayName("setFeatureHidden — display 없으면 NoSuchElementException")
    void shouldThrowWhenFeatureDisplayNotFound() {
        given(featureDisplayRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setFeatureHidden(USER_ID, FeatureCode.STOCK_SEARCH, true))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─── setModuleHidden ──────────────────────────────────────────────────

    @Test
    @DisplayName("setModuleHidden — hidden=false로 복원")
    void shouldUnhideModule() {
        UserModuleDisplay display = moduleDisplay(ModuleCode.STOCK, 0);
        display.setHidden(true);
        given(moduleDisplayRepository.findByUserIdAndModuleCode(USER_ID, ModuleCode.STOCK))
                .willReturn(Optional.of(display));

        service.setModuleHidden(USER_ID, ModuleCode.STOCK, false);

        assertThat(display.isHidden()).isFalse();
    }
}
