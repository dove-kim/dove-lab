package com.dove.api.member.admin.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

/**
 * 관리 화면용 회원 요약 조합 조회 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class MemberSummaryQueryServiceTest {

    @Mock MemberProfileQueryService memberProfileQueryService;
    @Mock CredentialService credentialService;

    @InjectMocks MemberSummaryQueryService service;

    private static MemberProfile profileWithId(Long id, String email, String name, MemberRole role) {
        MemberProfile p = MemberProfile.create(email, name, role);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Nested
    @DisplayName("findAllSummaries")
    class FindAllSummaries {

        @Test
        @DisplayName("프로필과 username을 회원 ID로 묶어 요약을 만든다")
        void shouldCombineProfileAndUsernameWhenFound() {
            MemberProfile p1 = profileWithId(1L, "a@a.com", "에이", MemberRole.ADMIN);
            MemberProfile p2 = profileWithId(2L, "b@b.com", "비", MemberRole.USER);
            given(memberProfileQueryService.findAll()).willReturn(List.of(p1, p2));
            given(credentialService.findUsernamesByMemberIds(anyCollection()))
                    .willReturn(Map.of(1L, "userA", 2L, "userB"));

            List<MemberSummary> result = service.findAllSummaries();

            assertThat(result).containsExactly(
                    new MemberSummary(1L, "에이", "a@a.com", "userA", "ADMIN"),
                    new MemberSummary(2L, "비", "b@b.com", "userB", "USER"));
        }

        @Test
        @DisplayName("username 매핑이 없는 회원은 빈 문자열로 채운다")
        void shouldUseEmptyUsernameWhenMappingMissing() {
            MemberProfile p1 = profileWithId(1L, "a@a.com", "에이", MemberRole.USER);
            given(memberProfileQueryService.findAll()).willReturn(List.of(p1));
            given(credentialService.findUsernamesByMemberIds(anyCollection())).willReturn(Map.of());

            List<MemberSummary> result = service.findAllSummaries();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).username()).isEmpty();
        }

        @Test
        @DisplayName("회원이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenNoMembers() {
            given(memberProfileQueryService.findAll()).willReturn(List.of());
            given(credentialService.findUsernamesByMemberIds(anyCollection())).willReturn(Map.of());

            assertThat(service.findAllSummaries()).isEmpty();
        }
    }
}
