package com.dove.user.domain.entity;

import com.dove.auth.domain.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberProfile")
class MemberProfileTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("shouldSetAllFieldsAndCreatedAt")
        void shouldSetAllFieldsAndCreatedAt() {
            MemberProfile profile = MemberProfile.create("alice@example.com", "Alice", MemberRole.USER);

            assertThat(profile.getEmail()).isEqualTo("alice@example.com");
            assertThat(profile.getName()).isEqualTo("Alice");
            assertThat(profile.getRole()).isEqualTo(MemberRole.USER);
            assertThat(profile.getCreatedAt()).isNotNull();
            assertThat(profile.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("shouldUpdateRoleToNewValue")
        void shouldUpdateRoleToNewValue() {
            MemberProfile profile = MemberProfile.create("user@example.com", "User", MemberRole.USER);

            profile.changeRole(MemberRole.ADMIN);

            assertThat(profile.getRole()).isEqualTo(MemberRole.ADMIN);
        }
    }
}
