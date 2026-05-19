package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.entity.UserFeatureGrant;
import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.UserFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.UserFeatureGrantRepository;
import com.dove.userfeature.domain.repository.UserModuleDisplayRepository;
import org.junit.jupiter.api.DisplayName;
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
class UserFeatureGrantCommandServiceTest {

    @Mock UserFeatureGrantRepository grantRepository;
    @Mock UserFeatureDisplayRepository featureDisplayRepository;
    @Mock UserModuleDisplayRepository moduleDisplayRepository;
    @Mock UserSubMenuGrantCommandService subMenuGrantCommandService;
    @InjectMocks UserFeatureGrantCommandService service;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 99L;

    // ─── grant ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("grant — 신규 기능: grant 생성 + display 설정 자동 생성")
    void shouldCreateGrantAndDisplayOnNewFeature() {
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.empty());
        given(featureDisplayRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.empty());
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(moduleDisplayRepository.findByUserIdAndModuleCode(USER_ID, ModuleCode.STOCK))
                .willReturn(Optional.empty());
        given(moduleDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(featureDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(moduleDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));

        service.grant(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

        verify(grantRepository).save(any(UserFeatureGrant.class));
        verify(featureDisplayRepository).save(any(UserFeatureDisplay.class));
        verify(moduleDisplayRepository).save(any(UserModuleDisplay.class));
    }

    @Test
    @DisplayName("grant — 이미 활성: 아무 변경 없음")
    void shouldSkipWhenAlreadyActive() {
        UserFeatureGrant existing = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(existing));

        service.grant(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

        verify(grantRepository, never()).save(any());
    }

    @Test
    @DisplayName("grant — 회수됐던 기능 재부여: activate 호출")
    void shouldReactivateRevokedGrant() {
        UserFeatureGrant revoked = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
        revoked.revoke();
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(revoked));
        given(featureDisplayRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(UserFeatureDisplay.create(USER_ID, FeatureCode.STOCK_SEARCH, 0)));
        given(moduleDisplayRepository.findByUserIdAndModuleCode(USER_ID, ModuleCode.STOCK))
                .willReturn(Optional.of(UserModuleDisplay.create(USER_ID, ModuleCode.STOCK, 0)));
        given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));

        service.grant(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

        assertThat(revoked.isActive()).isTrue();
        verify(grantRepository).save(revoked);
    }

    @Test
    @DisplayName("grant — 두 번째 기능 부여 시 displayOrder = 1")
    void shouldIncrementDisplayOrderForSecondFeature() {
        UserFeatureDisplay existing = UserFeatureDisplay.create(USER_ID, FeatureCode.STOCK_SEARCH, 0);
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_LEDGER))
                .willReturn(Optional.empty());
        given(featureDisplayRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_LEDGER))
                .willReturn(Optional.empty());
        given(featureDisplayRepository.findAllByUserId(USER_ID)).willReturn(List.of(existing));
        given(moduleDisplayRepository.findByUserIdAndModuleCode(USER_ID, ModuleCode.STOCK))
                .willReturn(Optional.of(UserModuleDisplay.create(USER_ID, ModuleCode.STOCK, 0)));
        given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(featureDisplayRepository.save(any())).willAnswer(i -> i.getArgument(0));

        service.grant(USER_ID, FeatureCode.STOCK_LEDGER, ADMIN_ID);

        ArgumentCaptor<UserFeatureDisplay> captor = ArgumentCaptor.forClass(UserFeatureDisplay.class);
        verify(featureDisplayRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
    }

    // ─── revoke ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("revoke — 활성 grant 회수")
    void shouldRevokeActiveGrant() {
        UserFeatureGrant grant = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(grant));
        given(grantRepository.save(any())).willAnswer(i -> i.getArgument(0));

        service.revoke(USER_ID, FeatureCode.STOCK_SEARCH);

        assertThat(grant.isActive()).isFalse();
        verify(grantRepository).save(grant);
    }

    @Test
    @DisplayName("revoke — grant 없으면 아무 동작 없음")
    void shouldDoNothingWhenGrantNotFound() {
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.empty());

        service.revoke(USER_ID, FeatureCode.STOCK_SEARCH);

        verify(grantRepository, never()).save(any());
    }
}
