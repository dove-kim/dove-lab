package com.dove.auth.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialTest {

    @Test
    @DisplayName("create — 초기 failedAttempts=0, lockedUntil=null")
    void shouldCreateWithZeroFailedAttempts() {
        Credential c = Credential.create(1L, "user01", "hash");

        assertThat(c.getMemberId()).isEqualTo(1L);
        assertThat(c.getUsername()).isEqualTo("user01");
        assertThat(c.getPasswordHash()).isEqualTo("hash");
        assertThat(c.getFailedAttempts()).isZero();
        assertThat(c.getLockedUntil()).isNull();
        assertThat(c.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("recordSuccessfulLogin — lastLoginAt 갱신, failedAttempts 초기화, lockedUntil 해제")
    void shouldResetOnSuccessfulLogin() {
        Credential c = Credential.create(1L, "user01", "hash");
        c.recordFailedLogin();
        c.recordFailedLogin();
        c.lockUntil(LocalDateTime.now().plusMinutes(10));

        c.recordSuccessfulLogin();

        assertThat(c.getFailedAttempts()).isZero();
        assertThat(c.getLockedUntil()).isNull();
        assertThat(c.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("recordFailedLogin — 누적 호출 시마다 failedAttempts 증가")
    void shouldIncrementFailedAttempts() {
        Credential c = Credential.create(1L, "user01", "hash");

        c.recordFailedLogin();
        c.recordFailedLogin();
        c.recordFailedLogin();

        assertThat(c.getFailedAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("isLocked — lockedUntil이 미래이면 true")
    void shouldBeLockedWhenLockedUntilIsFuture() {
        Credential c = Credential.create(1L, "user01", "hash");
        c.lockUntil(LocalDateTime.now().plusMinutes(5));

        assertThat(c.isLocked()).isTrue();
    }

    @Test
    @DisplayName("isLocked — lockedUntil이 과거이면 false")
    void shouldNotBeLockedWhenLockedUntilIsPast() {
        Credential c = Credential.create(1L, "user01", "hash");
        c.lockUntil(LocalDateTime.now().minusSeconds(1));

        assertThat(c.isLocked()).isFalse();
    }

    @Test
    @DisplayName("isLocked — lockedUntil이 null이면 false")
    void shouldNotBeLockedWhenLockedUntilIsNull() {
        Credential c = Credential.create(1L, "user01", "hash");

        assertThat(c.isLocked()).isFalse();
    }

    @Test
    @DisplayName("lockUntil — 설정 후 isLocked=true, 성공 로그인 후 isLocked=false")
    void shouldUnlockAfterSuccessfulLogin() {
        Credential c = Credential.create(1L, "user01", "hash");
        c.lockUntil(LocalDateTime.now().plusHours(1));
        assertThat(c.isLocked()).isTrue();

        c.recordSuccessfulLogin();

        assertThat(c.isLocked()).isFalse();
    }
}
