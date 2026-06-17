package com.dove.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-that-is-32bytes!!";
    private static final long ACCESS_MS = 900_000L;
    private static final long REFRESH_MS = 2_592_000_000L;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_MS, REFRESH_MS);
    }

    @Test
    @DisplayName("access token — memberId/username/role/features 추출 가능")
    void shouldExtractAccessClaims() {
        String token = jwtProvider.generateAccessToken(
                42L, "alice", "Alice", "USER", false, Set.of("STOCK_SEARCH"));

        assertThat(jwtProvider.extractMemberId(token)).isEqualTo(42L);
        assertThat(jwtProvider.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtProvider.extractRole(token)).isEqualTo("USER");
        assertThat(jwtProvider.extractMustChangePassword(token)).isFalse();
        assertThat(jwtProvider.extractTokenType(token)).isEqualTo(JwtProvider.TOKEN_TYPE_ACCESS);
        assertThat(jwtProvider.extractCapabilities(token)).containsExactly("STOCK_SEARCH");
        assertThat(jwtProvider.extractIssuedAt(token)).isNotNull();
    }

    @Test
    @DisplayName("access token — features null/empty 허용")
    void shouldAllowEmptyFeatures() {
        String token = jwtProvider.generateAccessToken(1L, "alice", "Alice", "USER", false, null);

        assertThat(jwtProvider.extractCapabilities(token)).isEmpty();
    }

    @Test
    @DisplayName("refresh token — tokenType=REFRESH, memberId 추출 가능")
    void shouldGenerateRefreshToken() {
        String token = jwtProvider.generateRefreshToken(7L);

        assertThat(jwtProvider.extractTokenType(token)).isEqualTo(JwtProvider.TOKEN_TYPE_REFRESH);
        assertThat(jwtProvider.extractMemberId(token)).isEqualTo(7L);
        assertThat(jwtProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("validate — 유효한 토큰은 true")
    void shouldValidateValidToken() {
        String token = jwtProvider.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());

        assertThat(jwtProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("validate — 만료된 토큰은 false")
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtProvider shortLived = new JwtProvider(SECRET, 1L, 1L);
        String token = shortLived.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());
        Thread.sleep(20);

        assertThat(shortLived.validate(token)).isFalse();
    }

    @Test
    @DisplayName("validate — 변조된 토큰은 false")
    void shouldRejectTamperedToken() {
        String token = jwtProvider.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());
        String tampered = token.substring(0, token.length() - 6) + "TAMPER";

        assertThat(jwtProvider.validate(tampered)).isFalse();
    }

    @Test
    @DisplayName("validate — 다른 키로 서명된 토큰은 false")
    void shouldRejectTokenSignedWithDifferentKey() {
        JwtProvider otherProvider = new JwtProvider(
                "other-secret-key-that-is-32bytes!!", ACCESS_MS, REFRESH_MS);
        String token = otherProvider.generateAccessToken(1L, "alice", "Alice", "USER", false, Set.of());

        assertThat(jwtProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("validate — 빈 문자열은 false")
    void shouldRejectEmptyToken() {
        assertThat(jwtProvider.validate("")).isFalse();
    }
}
