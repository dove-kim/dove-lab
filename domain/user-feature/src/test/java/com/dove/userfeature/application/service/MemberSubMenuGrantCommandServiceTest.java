package com.dove.userfeature.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.userfeature.domain.entity.MemberSubMenuGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.MemberSubMenuGrantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSubMenuGrantCommandService")
class MemberSubMenuGrantCommandServiceTest {

    @Mock MemberSubMenuGrantRepository repository;
    @Mock ForcedLogoutService forcedLogoutService;
    @InjectMocks MemberSubMenuGrantCommandService service;

    private static final Long MEMBER_ID = 1L;
    private static final Long ADMIN_ID = 99L;

    @Nested
    @DisplayName("grant")
    class Grant {

        @Test
        @DisplayName("shouldCreateGrantAndMarkLogoutWhenNotFound")
        void shouldCreateGrantAndMarkLogoutWhenNotFound() {
            given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN))
                    .willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grant(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);

            verify(repository).save(any(MemberSubMenuGrant.class));
            verify(forcedLogoutService).markLogoutNow(MEMBER_ID);
        }

        @Test
        @DisplayName("shouldSkipSaveWhenAlreadyActive")
        void shouldSkipSaveWhenAlreadyActive() {
            MemberSubMenuGrant active = MemberSubMenuGrant.create(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);
            given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN))
                    .willReturn(Optional.of(active));

            service.grant(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);

            verify(repository, never()).save(any());
            verify(forcedLogoutService).markLogoutNow(MEMBER_ID);
        }

        @Test
        @DisplayName("shouldReactivateAndMarkLogoutWhenRevoked")
        void shouldReactivateAndMarkLogoutWhenRevoked() {
            MemberSubMenuGrant revoked = MemberSubMenuGrant.create(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);
            revoked.revoke();
            given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN))
                    .willReturn(Optional.of(revoked));
            given(repository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grant(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);

            assertThat(revoked.isActive()).isTrue();
            verify(repository).save(revoked);
            verify(forcedLogoutService).markLogoutNow(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("shouldRevokeAndMarkLogoutWhenGrantExists")
        void shouldRevokeAndMarkLogoutWhenGrantExists() {
            MemberSubMenuGrant grant = MemberSubMenuGrant.create(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, ADMIN_ID);
            given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN))
                    .willReturn(Optional.of(grant));
            given(repository.save(any())).willAnswer(i -> i.getArgument(0));

            service.revoke(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN);

            assertThat(grant.isActive()).isFalse();
            verify(repository).save(grant);
            verify(forcedLogoutService).markLogoutNow(MEMBER_ID);
        }

        @Test
        @DisplayName("shouldDoNothingWhenGrantNotFound")
        void shouldDoNothingWhenGrantNotFound() {
            given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN))
                    .willReturn(Optional.empty());

            service.revoke(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN);

            verify(repository, never()).save(any());
            verify(forcedLogoutService).markLogoutNow(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("grantAll")
    class GrantAll {

        @Test
        @DisplayName("shouldGrantAllSubMenusForFeatureWithoutMarkingLogout")
        void shouldGrantAllSubMenusForFeatureWithoutMarkingLogout() {
            int subMenuCount = SubMenuCode.byFeature(FeatureCode.STOCK_SEARCH).size();
            given(repository.findByMemberIdAndSubMenuCode(any(), any())).willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(i -> i.getArgument(0));

            service.grantAll(MEMBER_ID, FeatureCode.STOCK_SEARCH, ADMIN_ID);

            // grantAll은 개별 메뉴마다 강제 로그아웃을 발생시키지 않는다
            verify(forcedLogoutService, never()).markLogoutNow(any());
            // STOCK_SEARCH에 속한 하위 메뉴 수만큼 저장
            verify(repository, org.mockito.Mockito.times(subMenuCount)).save(any(MemberSubMenuGrant.class));
        }
    }

    @Nested
    @DisplayName("revokeAll")
    class RevokeAll {

        @Test
        @DisplayName("shouldRevokeAllSubMenusForFeatureWithoutMarkingLogout")
        void shouldRevokeAllSubMenusForFeatureWithoutMarkingLogout() {
            SubMenuCode.byFeature(FeatureCode.STOCK_SEARCH).forEach(sub -> {
                MemberSubMenuGrant grant = MemberSubMenuGrant.create(MEMBER_ID, sub, ADMIN_ID);
                given(repository.findByMemberIdAndSubMenuCode(MEMBER_ID, sub))
                        .willReturn(Optional.of(grant));
                given(repository.save(any())).willAnswer(i -> i.getArgument(0));
            });

            service.revokeAll(MEMBER_ID, FeatureCode.STOCK_SEARCH);

            verify(forcedLogoutService, never()).markLogoutNow(any());
        }
    }
}
