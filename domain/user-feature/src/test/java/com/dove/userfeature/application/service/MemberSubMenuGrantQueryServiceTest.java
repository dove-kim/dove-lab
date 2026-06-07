package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberSubMenuGrant;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.MemberSubMenuGrantRepository;
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
@DisplayName("MemberSubMenuGrantQueryService")
class MemberSubMenuGrantQueryServiceTest {

    @Mock MemberSubMenuGrantRepository repository;
    @InjectMocks MemberSubMenuGrantQueryService service;

    private static final Long MEMBER_ID = 1L;

    @Nested
    @DisplayName("findActiveSubMenuCodes")
    class FindActiveSubMenuCodes {

        @Test
        @DisplayName("shouldReturnActiveSubMenuCodes")
        void shouldReturnActiveSubMenuCodes() {
            MemberSubMenuGrant grant = MemberSubMenuGrant.create(MEMBER_ID, SubMenuCode.STOCK_SEARCH_MAIN, null);
            given(repository.findAllByMemberIdAndActiveTrue(MEMBER_ID)).willReturn(List.of(grant));

            Set<SubMenuCode> result = service.findActiveSubMenuCodes(MEMBER_ID);

            assertThat(result).containsExactly(SubMenuCode.STOCK_SEARCH_MAIN);
        }

        @Test
        @DisplayName("shouldReturnEmptySetWhenNoActiveGrants")
        void shouldReturnEmptySetWhenNoActiveGrants() {
            given(repository.findAllByMemberIdAndActiveTrue(MEMBER_ID)).willReturn(List.of());

            assertThat(service.findActiveSubMenuCodes(MEMBER_ID)).isEmpty();
        }
    }
}
