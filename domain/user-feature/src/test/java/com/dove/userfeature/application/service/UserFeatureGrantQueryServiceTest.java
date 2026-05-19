package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserFeatureGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.repository.UserFeatureGrantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserFeatureGrantQueryServiceTest {

    @Mock UserFeatureGrantRepository grantRepository;
    @InjectMocks UserFeatureGrantQueryService service;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("findActiveFeatureCodes — 활성 기능 코드 반환")
    void shouldReturnActiveFeatureCodes() {
        UserFeatureGrant grant = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, null);
        given(grantRepository.findAllByUserIdAndActiveTrue(USER_ID)).willReturn(List.of(grant));

        Set<FeatureCode> result = service.findActiveFeatureCodes(USER_ID);

        assertThat(result).containsExactly(FeatureCode.STOCK_SEARCH);
    }

    @Test
    @DisplayName("findActiveFeatureCodes — 활성 기능 없으면 빈 Set")
    void shouldReturnEmptySetWhenNoActiveFeatures() {
        given(grantRepository.findAllByUserIdAndActiveTrue(USER_ID)).willReturn(List.of());

        assertThat(service.findActiveFeatureCodes(USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("hasActiveGrant — 활성 grant 있으면 true")
    void shouldReturnTrueWhenActiveGrantExists() {
        UserFeatureGrant grant = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, null);
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(grant));

        assertThat(service.hasActiveGrant(USER_ID, FeatureCode.STOCK_SEARCH)).isTrue();
    }

    @Test
    @DisplayName("hasActiveGrant — 회수된 grant면 false")
    void shouldReturnFalseWhenGrantRevoked() {
        UserFeatureGrant grant = UserFeatureGrant.create(USER_ID, FeatureCode.STOCK_SEARCH, null);
        grant.revoke();
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.of(grant));

        assertThat(service.hasActiveGrant(USER_ID, FeatureCode.STOCK_SEARCH)).isFalse();
    }

    @Test
    @DisplayName("hasActiveGrant — grant 없으면 false")
    void shouldReturnFalseWhenGrantNotFound() {
        given(grantRepository.findByUserIdAndFeatureCode(USER_ID, FeatureCode.STOCK_SEARCH))
                .willReturn(Optional.empty());

        assertThat(service.hasActiveGrant(USER_ID, FeatureCode.STOCK_SEARCH)).isFalse();
    }
}
