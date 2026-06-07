package com.dove.userfeature.application.service;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberMenuDisplayCommandService")
class MemberMenuDisplayCommandServiceTest {

    @Mock MemberFeatureDisplayRepository featureDisplayRepository;
    @Mock MemberModuleDisplayRepository moduleDisplayRepository;
    @InjectMocks MemberMenuDisplayCommandService service;

    private static final Long MEMBER_ID = 1L;

    private MemberModuleDisplay moduleDisplay(ModuleCode code, int order) {
        MemberModuleDisplay d = MemberModuleDisplay.create(MEMBER_ID, code, order);
        ReflectionTestUtils.setField(d, "id", (long) order + 1);
        return d;
    }

    private MemberFeatureDisplay featureDisplay(FeatureCode code, int order) {
        return MemberFeatureDisplay.create(MEMBER_ID, code, order);
    }

    @Nested
    @DisplayName("reorderModules")
    class ReorderModules {

        @Test
        @DisplayName("shouldReorderModulesInGivenOrder")
        void shouldReorderModulesInGivenOrder() {
            MemberModuleDisplay stock = moduleDisplay(ModuleCode.STOCK, 1);
            MemberModuleDisplay budget = moduleDisplay(ModuleCode.BUDGET, 0);
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(stock, budget));

            service.reorderModules(MEMBER_ID, List.of(ModuleCode.STOCK, ModuleCode.BUDGET));

            assertThat(stock.getDisplayOrder()).isEqualTo(0);
            assertThat(budget.getDisplayOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("reorderFeatures")
    class ReorderFeatures {

        @Test
        @DisplayName("shouldReorderFeaturesWithinModule")
        void shouldReorderFeaturesWithinModule() {
            MemberFeatureDisplay search = featureDisplay(FeatureCode.STOCK_SEARCH, 1);
            MemberFeatureDisplay ledger = featureDisplay(FeatureCode.STOCK_LEDGER, 0);
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(search, ledger));

            service.reorderFeatures(MEMBER_ID, ModuleCode.STOCK,
                    List.of(FeatureCode.STOCK_SEARCH, FeatureCode.STOCK_LEDGER));

            assertThat(search.getDisplayOrder()).isEqualTo(0);
            assertThat(ledger.getDisplayOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("setFeatureHidden")
    class SetFeatureHidden {

        @Test
        @DisplayName("shouldSetFeatureHiddenTrue")
        void shouldSetFeatureHiddenTrue() {
            MemberFeatureDisplay display = featureDisplay(FeatureCode.STOCK_SEARCH, 0);
            given(featureDisplayRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(display));

            service.setFeatureHidden(MEMBER_ID, FeatureCode.STOCK_SEARCH, true);

            assertThat(display.isHidden()).isTrue();
        }

        @Test
        @DisplayName("shouldThrowWhenFeatureDisplayNotFound")
        void shouldThrowWhenFeatureDisplayNotFound() {
            given(featureDisplayRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.setFeatureHidden(MEMBER_ID, FeatureCode.STOCK_SEARCH, true))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("setModuleHidden")
    class SetModuleHidden {

        @Test
        @DisplayName("shouldUnhideModule")
        void shouldUnhideModule() {
            MemberModuleDisplay display = moduleDisplay(ModuleCode.STOCK, 0);
            display.setHidden(true);
            given(moduleDisplayRepository.findByMemberIdAndModuleCode(MEMBER_ID, ModuleCode.STOCK))
                    .willReturn(Optional.of(display));

            service.setModuleHidden(MEMBER_ID, ModuleCode.STOCK, false);

            assertThat(display.isHidden()).isFalse();
        }
    }
}
