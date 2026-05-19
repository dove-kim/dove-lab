package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberProfileQueryServiceTest {

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @InjectMocks
    private MemberProfileQueryService queryService;

    @Test
    @DisplayName("findById — 프로필 반환")
    void shouldFindById() {
        MemberProfile profile = MemberProfile.create("a@a.com", "Alice", MemberRole.USER);
        given(memberProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        Optional<MemberProfile> result = queryService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("a@a.com");
    }

    @Test
    @DisplayName("findById — 없으면 empty")
    void shouldReturnEmptyWhenNotFound() {
        given(memberProfileRepository.findById(99L)).willReturn(Optional.empty());

        assertThat(queryService.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail — 이메일 존재 여부 전달")
    void shouldDelegateExistsByEmail() {
        given(memberProfileRepository.existsByEmail("a@a.com")).willReturn(true);
        given(memberProfileRepository.existsByEmail("b@b.com")).willReturn(false);

        assertThat(queryService.existsByEmail("a@a.com")).isTrue();
        assertThat(queryService.existsByEmail("b@b.com")).isFalse();
    }

    @Test
    @DisplayName("existsAdmin — ADMIN 여부 전달")
    void shouldCheckAdminExistence() {
        given(memberProfileRepository.existsByRole(MemberRole.ADMIN)).willReturn(true);

        assertThat(queryService.existsAdmin()).isTrue();
    }

    @Test
    @DisplayName("existsAdmin — ADMIN 없으면 false")
    void shouldReturnFalseWhenNoAdmin() {
        given(memberProfileRepository.existsByRole(MemberRole.ADMIN)).willReturn(false);

        assertThat(queryService.existsAdmin()).isFalse();
    }
}
