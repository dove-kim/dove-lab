package com.dove.user.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
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

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberProfileCommandService")
class MemberProfileCommandServiceTest {

    @Mock MemberProfileRepository memberProfileRepository;
    @Mock ForcedLogoutService forcedLogoutService;
    @InjectMocks MemberProfileCommandService commandService;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("shouldDelegateSaveToRepository")
        void shouldDelegateSaveToRepository() {
            MemberProfile profile = MemberProfile.create("a@a.com", "Alice", MemberRole.USER);
            given(memberProfileRepository.save(profile)).willReturn(profile);

            MemberProfile result = commandService.save(profile);

            assertThat(result.getEmail()).isEqualTo("a@a.com");
        }
    }

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("shouldChangeRoleAndMarkLogout")
        void shouldChangeRoleAndMarkLogout() {
            MemberProfile profile = MemberProfile.create("user@example.com", "User", MemberRole.USER);
            given(memberProfileRepository.findById(1L)).willReturn(Optional.of(profile));
            given(memberProfileRepository.save(any())).willAnswer(i -> i.getArgument(0));

            MemberProfile result = commandService.changeRole(1L, MemberRole.ADMIN);

            assertThat(result.getRole()).isEqualTo(MemberRole.ADMIN);
            verify(forcedLogoutService).markLogoutNow(1L);
        }

        @Test
        @DisplayName("shouldThrowIllegalStateWhenCurrentRoleIsRoot")
        void shouldThrowIllegalStateWhenCurrentRoleIsRoot() {
            MemberProfile root = MemberProfile.create("root@example.com", "Root", MemberRole.ROOT);
            given(memberProfileRepository.findById(1L)).willReturn(Optional.of(root));

            assertThatThrownBy(() -> commandService.changeRole(1L, MemberRole.ADMIN))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("ROOT_ROLE_IMMUTABLE");

            verify(memberProfileRepository, never()).save(any());
            verify(forcedLogoutService, never()).markLogoutNow(any());
        }

        @Test
        @DisplayName("shouldThrowIllegalArgumentWhenNewRoleIsRoot")
        void shouldThrowIllegalArgumentWhenNewRoleIsRoot() {
            MemberProfile profile = MemberProfile.create("admin@example.com", "Admin", MemberRole.ADMIN);
            given(memberProfileRepository.findById(1L)).willReturn(Optional.of(profile));

            assertThatThrownBy(() -> commandService.changeRole(1L, MemberRole.ROOT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ROOT_ROLE_IMMUTABLE");

            verify(memberProfileRepository, never()).save(any());
            verify(forcedLogoutService, never()).markLogoutNow(any());
        }

        @Test
        @DisplayName("shouldThrowNoSuchElementWhenMemberNotFound")
        void shouldThrowNoSuchElementWhenMemberNotFound() {
            given(memberProfileRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.changeRole(99L, MemberRole.ADMIN))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }
}
