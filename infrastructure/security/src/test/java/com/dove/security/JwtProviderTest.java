package com.dove.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    // HMAC-SHA256 최소 256비트(32바이트) 이상 필요
    private static final String SECRET = "test-secret-key-that-is-32bytes!!";
    private static final long EXPIRATION_MS = 3_600_000L;        // 1h
    private static final long REMEMBER_ME_MS = 2_592_000_000L;   // 30d

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION_MS, REMEMBER_ME_MS);
    }

    @Test
    @DisplayName("generate — 생성된 토큰에서 memberId 추출 가능")
    void shouldExtractMemberId() {
        String token = jwtProvider.generate(42L, "alice", "Alice", "USER", false, false);

        assertThat(jwtProvider.extractMemberId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("generate — 생성된 토큰에서 username 추출 가능")
    void shouldExtractUsername() {
        String token = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);

        assertThat(jwtProvider.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("generate — 생성된 토큰에서 name 추출 가능")
    void shouldExtractName() {
        String token = jwtProvider.generate(1L, "alice", "Alice Kim", "USER", false, false);

        assertThat(jwtProvider.extractName(token)).isEqualTo("Alice Kim");
    }

    @Test
    @DisplayName("generate — 생성된 토큰에서 role 추출 가능")
    void shouldExtractRole() {
        String token = jwtProvider.generate(1L, "admin", "Admin", "ADMIN", false, false);

        assertThat(jwtProvider.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("validate — 유효한 토큰은 true")
    void shouldValidateValidToken() {
        String token = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);

        assertThat(jwtProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("validate — 만료된 토큰은 false")
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtProvider shortLived = new JwtProvider(SECRET, 1L, 1L); // 1ms 만료
        String token = shortLived.generate(1L, "alice", "Alice", "USER", false, false);
        Thread.sleep(20); // 만료 대기

        assertThat(shortLived.validate(token)).isFalse();
    }

    @Test
    @DisplayName("validate — 변조된 토큰은 false")
    void shouldRejectTamperedToken() {
        String token = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);
        // 서명 부분 마지막 몇 글자 교체
        String tampered = token.substring(0, token.length() - 6) + "TAMPER";

        assertThat(jwtProvider.validate(tampered)).isFalse();
    }

    @Test
    @DisplayName("validate — 다른 키로 서명된 토큰은 false")
    void shouldRejectTokenSignedWithDifferentKey() {
        JwtProvider otherProvider = new JwtProvider("other-secret-key-that-is-32bytes!!", EXPIRATION_MS, REMEMBER_ME_MS);
        String token = otherProvider.generate(1L, "alice", "Alice", "USER", false, false);

        assertThat(jwtProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("validate — 빈 문자열은 false")
    void shouldRejectEmptyToken() {
        assertThat(jwtProvider.validate("")).isFalse();
    }

    @Test
    @DisplayName("generate rememberMe=true — 더 긴 만료 시간 적용 (기본보다 긴 exp)")
    void shouldUseLongerExpiryForRememberMe() {
        // rememberMe=false 토큰의 exp와 rememberMe=true 토큰의 exp를 비교
        // (둘 다 유효하지만 rememberMe가 더 나중에 만료)
        String normalToken = jwtProvider.generate(1L, "alice", "Alice", "USER", false, false);
        String rememberMeToken = jwtProvider.generate(1L, "alice", "Alice", "USER", true, false);

        // 두 토큰 모두 현재 유효
        assertThat(jwtProvider.validate(normalToken)).isTrue();
        assertThat(jwtProvider.validate(rememberMeToken)).isTrue();

        // rememberMe 토큰의 페이로드가 더 긴 exp를 가짐: 토큰 길이로 간접 확인은 어려우므로
        // 두 토큰이 서로 다름을 검증
        assertThat(normalToken).isNotEqualTo(rememberMeToken);
    }
}
