package com.dove.auth.domain.entity;

import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InviteCodeTest {

    @Test
    @DisplayName("create — 코드 32자 hex UUID, 사용일시 null")
    void shouldCreateWithUnusedState() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        InviteCode code = InviteCode.create(MemberRole.USER, expiry, "admin");

        assertThat(code.getCode()).hasSize(32);
        assertThat(code.getRole()).isEqualTo(MemberRole.USER);
        assertThat(code.getExpiresAt()).isEqualTo(expiry);
        assertThat(code.getCreatedBy()).isEqualTo("admin");
        assertThat(code.getUsedAt()).isNull();
        assertThat(code.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("isValid — 미사용 + 만료 전 → true")
    void shouldBeValidWhenUnusedAndNotExpired() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");

        assertThat(code.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid — 만료된 코드 → false")
    void shouldNotBeValidWhenExpired() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().minusSeconds(1), "admin");

        assertThat(code.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid — 사용된 코드 → false")
    void shouldNotBeValidAfterUse() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
        code.use();

        assertThat(code.isValid()).isFalse();
    }

    @Test
    @DisplayName("use — usedAt 설정됨")
    void shouldSetUsedAtOnUse() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");

        code.use();

        assertThat(code.getUsedAt()).isNotNull();
        assertThat(code.getUsedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("create — ADMIN 권한 코드 생성")
    void shouldCreateAdminCode() {
        InviteCode code = InviteCode.create(MemberRole.ADMIN, LocalDateTime.now().plusDays(7), "superadmin");

        assertThat(code.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(code.isValid()).isTrue();
    }

    @Test
    @DisplayName("create — 호출마다 다른 코드 값 생성")
    void shouldGenerateUniqueCodesEachTime() {
        InviteCode code1 = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");
        InviteCode code2 = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(1), "admin");

        assertThat(code1.getCode()).isNotEqualTo(code2.getCode());
    }
}
