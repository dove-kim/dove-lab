package com.dove.userfeature.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.entity.MemberFeatureGrant;
import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.MemberFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.MemberFeatureGrantRepository;
import com.dove.userfeature.domain.repository.MemberModuleDisplayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberFeatureGrantCommandService")
class MemberFeatureGrantCommandServiceTest {

    @Mock MemberFeatureGrantRepository grantRepository;
    @Mock MemberFeatureDisplayRepository featureDisplayRepository;
    @Mock MemberModuleDisplayRepository moduleDisplayRepository;
    @Mock MemberSubMenuGrantCommandService subMenuGrantCommandService;
    @Mock ForcedLogoutService forcedLogoutService;
    @InjectMocks MemberFeatureGrantCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long ADMIN_ID = 99L;

    @Nested
    @DisplayName("grant")
    class Grant {

        @Test
        @DisplayName("shouldCreateGrantAndDisplayOnNewFeature")
        void shouldCreateGrantAndDisplayOnNewFeature() {
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.empty());
            given(featureDisplayRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.empty());
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());
            given(moduleDisplayRepository.findByMemberIdAndModuleCode(MEMBER_ID, ModuleCode.STOCK))
                    .willReturn(Optional.empty());
            given(moduleDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of());
            given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));
            given(featureDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));
            given(moduleDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

            verify(grantRepository).save(any(MemberFeatureGrant.class));
            verify(featureDisplayRepository).save(any(MemberFeatureDisplay.class));
            verify(moduleDisplayRepository).save(any(MemberModuleDisplay.class));
        }

        @Test
        @DisplayName("shouldSkipWhenAlreadyActive")
        void shouldSkipWhenAlreadyActive() {
            MemberFeatureGrant existing = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(existing));

            service.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

            verify(grantRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldReactivateRevokedGrant")
        void shouldReactivateRevokedGrant() {
            MemberFeatureGrant revoked = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
            revoked.revoke();
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(revoked));
            given(featureDisplayRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(MemberFeatureDisplay.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, 0)));
            given(moduleDisplayRepository.findByMemberIdAndModuleCode(MEMBER_ID, ModuleCode.STOCK))
                    .willReturn(Optional.of(MemberModuleDisplay.create(MEMBER_ID, ModuleCode.STOCK, 0)));
            given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grant(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

            assertThat(revoked.isActive()).isTrue();
            verify(grantRepository).save(revoked);
        }

        @Test
        @DisplayName("shouldIncrementDisplayOrderForSecondFeature")
        void shouldIncrementDisplayOrderForSecondFeature() {
            MemberFeatureDisplay existing = MemberFeatureDisplay.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, 0);
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_LEDGER))
                    .willReturn(Optional.empty());
            given(featureDisplayRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_LEDGER))
                    .willReturn(Optional.empty());
            given(featureDisplayRepository.findAllByMemberId(MEMBER_ID)).willReturn(List.of(existing));
            given(moduleDisplayRepository.findByMemberIdAndModuleCode(MEMBER_ID, ModuleCode.STOCK))
                    .willReturn(Optional.of(MemberModuleDisplay.create(MEMBER_ID, ModuleCode.STOCK, 0)));
            given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));
            given(featureDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grant(MEMBER_ID, FeatureCode.STOCK_LEDGER, ADMIN_ID);

            ArgumentCaptor<MemberFeatureDisplay> captor = ArgumentCaptor.forClass(MemberFeatureDisplay.class);
            verify(featureDisplayRepository).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("shouldRevokeActiveGrant")
        void shouldRevokeActiveGrant() {
            MemberFeatureGrant grant = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(grant));
            given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));

            service.revoke(MEMBER_ID, FeatureCode.STOCK_SEARCH);

            assertThat(grant.isActive()).isFalse();
            verify(grantRepository).save(grant);
        }

        @Test
        @DisplayName("shouldDoNothingWhenGrantNotFound")
        void shouldDoNothingWhenGrantNotFound() {
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.empty());

            service.revoke(MEMBER_ID, FeatureCode.STOCK_SEARCH);

            verify(grantRepository, never()).save(any());
        }
    }
}
