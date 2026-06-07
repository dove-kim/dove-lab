package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberProfileQueryService")
class MemberProfileQueryServiceTest {

    @Mock MemberProfileRepository memberProfileRepository;
    @InjectMocks MemberProfileQueryService queryService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("shouldReturnAllProfilesOrderedByCreatedAtDesc")
        void shouldReturnAllProfilesOrderedByCreatedAtDesc() {
            MemberProfile a = MemberProfile.create("a@a.com", "A", MemberRole.USER);
            MemberProfile b = MemberProfile.create("b@b.com", "B", MemberRole.ADMIN);
            given(memberProfileRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(b, a));

            List<MemberProfile> result = queryService.findAll();

            assertThat(result).containsExactly(b, a);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("shouldReturnProfileWhenFound")
        void shouldReturnProfileWhenFound() {
            MemberProfile profile = MemberProfile.create("a@a.com", "Alice", MemberRole.USER);
            given(memberProfileRepository.findById(1L)).willReturn(Optional.of(profile));

            Optional<MemberProfile> result = queryService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("a@a.com");
        }

        @Test
        @DisplayName("shouldReturnEmptyWhenNotFound")
        void shouldReturnEmptyWhenNotFound() {
            given(memberProfileRepository.findById(99L)).willReturn(Optional.empty());

            assertThat(queryService.findById(99L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("shouldDelegateToRepository")
        void shouldDelegateToRepository() {
            given(memberProfileRepository.existsByEmail("a@a.com")).willReturn(true);
            given(memberProfileRepository.existsByEmail("b@b.com")).willReturn(false);

            assertThat(queryService.existsByEmail("a@a.com")).isTrue();
            assertThat(queryService.existsByEmail("b@b.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsRoot")
    class ExistsRoot {

        @Test
        @DisplayName("shouldReturnTrueWhenRootExists")
        void shouldReturnTrueWhenRootExists() {
            given(memberProfileRepository.existsByRole(MemberRole.ROOT)).willReturn(true);

            assertThat(queryService.existsRoot()).isTrue();
        }

        @Test
        @DisplayName("shouldReturnFalseWhenNoRoot")
        void shouldReturnFalseWhenNoRoot() {
            given(memberProfileRepository.existsByRole(MemberRole.ROOT)).willReturn(false);

            assertThat(queryService.existsRoot()).isFalse();
        }
    }
}
