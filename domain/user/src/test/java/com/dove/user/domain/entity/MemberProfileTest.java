package com.dove.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileTest {

    @Test
    @DisplayName("create — 필드 정확히 설정, createdAt 자동 설정")
    void shouldCreateWithAllFields() {
        MemberProfile profile = MemberProfile.create("alice@example.com", "Alice", MemberRole.USER);

        assertThat(profile.getEmail()).isEqualTo("alice@example.com");
        assertThat(profile.getName()).isEqualTo("Alice");
        assertThat(profile.getRole()).isEqualTo(MemberRole.USER);
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getId()).isNull(); // 영속화 전
    }

    @Test
    @DisplayName("hasRole — 일치하는 권한이면 true")
    void shouldReturnTrueForMatchingRole() {
        MemberProfile profile = MemberProfile.create("admin@example.com", "Admin", MemberRole.ADMIN);

        assertThat(profile.hasRole(MemberRole.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("hasRole — 불일치하는 권한이면 false")
    void shouldReturnFalseForNonMatchingRole() {
        MemberProfile profile = MemberProfile.create("user@example.com", "User", MemberRole.USER);

        assertThat(profile.hasRole(MemberRole.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("create ADMIN — role ADMIN으로 생성")
    void shouldCreateAdminProfile() {
        MemberProfile profile = MemberProfile.create("admin@example.com", "Super Admin", MemberRole.ADMIN);

        assertThat(profile.hasRole(MemberRole.ADMIN)).isTrue();
        assertThat(profile.hasRole(MemberRole.USER)).isFalse();
    }
}
