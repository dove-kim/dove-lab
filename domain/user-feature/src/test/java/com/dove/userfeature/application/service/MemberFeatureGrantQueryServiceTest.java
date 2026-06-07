package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberFeatureGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.repository.MemberFeatureGrantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("MemberFeatureGrantQueryService")
class MemberFeatureGrantQueryServiceTest {

    @Mock MemberFeatureGrantRepository grantRepository;
    @InjectMocks MemberFeatureGrantQueryService service;

    private static final Long MEMBER_ID = 1L;

    @Nested
    @DisplayName("findActiveFeatureCodes")
    class FindActiveFeatureCodes {

        @Test
        @DisplayName("shouldReturnActiveFeatureCodes")
        void shouldReturnActiveFeatureCodes() {
            MemberFeatureGrant grant = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);
            given(grantRepository.findAllByMemberIdAndActiveTrue(MEMBER_ID)).willReturn(List.of(grant));

            Set<FeatureCode> result = service.findActiveFeatureCodes(MEMBER_ID);

            assertThat(result).containsExactly(FeatureCode.STOCK_SEARCH);
        }

        @Test
        @DisplayName("shouldReturnEmptySetWhenNoActiveFeatures")
        void shouldReturnEmptySetWhenNoActiveFeatures() {
            given(grantRepository.findAllByMemberIdAndActiveTrue(MEMBER_ID)).willReturn(List.of());

            assertThat(service.findActiveFeatureCodes(MEMBER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasActiveGrant")
    class HasActiveGrant {

        @Test
        @DisplayName("shouldReturnTrueWhenActiveGrantExists")
        void shouldReturnTrueWhenActiveGrantExists() {
            MemberFeatureGrant grant = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(grant));

            assertThat(service.hasActiveGrant(MEMBER_ID, FeatureCode.STOCK_SEARCH)).isTrue();
        }

        @Test
        @DisplayName("shouldReturnFalseWhenGrantRevoked")
        void shouldReturnFalseWhenGrantRevoked() {
            MemberFeatureGrant grant = MemberFeatureGrant.create(MEMBER_ID, FeatureCode.STOCK_SEARCH, null);
            grant.revoke();
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.of(grant));

            assertThat(service.hasActiveGrant(MEMBER_ID, FeatureCode.STOCK_SEARCH)).isFalse();
        }

        @Test
        @DisplayName("shouldReturnFalseWhenGrantNotFound")
        void shouldReturnFalseWhenGrantNotFound() {
            given(grantRepository.findByMemberIdAndFeatureCode(MEMBER_ID, FeatureCode.STOCK_SEARCH))
                    .willReturn(Optional.empty());

            assertThat(service.hasActiveGrant(MEMBER_ID, FeatureCode.STOCK_SEARCH)).isFalse();
        }
    }
}
