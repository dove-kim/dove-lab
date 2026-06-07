package com.dove.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForcedLogoutServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> ops;
    private ForcedLogoutService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> opsMock = mock(ValueOperations.class);
        ops = opsMock;
        when(redisTemplate.opsForValue()).thenReturn(ops);
        service = new ForcedLogoutService(redisTemplate);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 2_592_000_000L);
    }

    @Test
    @DisplayName("markLogoutNow → Redis 에 epoch ms 저장")
    void shouldMarkLogoutNow() {
        service.markLogoutNow(42L);

        verify(redisTemplate.opsForValue()).set(eq("auth:logout-after:42"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("isLoggedOut — cutoff 없음 → false")
    void shouldReturnFalseWhenNoCutoff() {
        when(ops.get("auth:logout-after:42")).thenReturn(null);

        assertThat(service.isLoggedOut(42L, Instant.now())).isFalse();
    }

    @Test
    @DisplayName("isLoggedOut — 토큰 iat < cutoff → true")
    void shouldReturnTrueWhenTokenBeforeCutoff() {
        long now = System.currentTimeMillis();
        when(ops.get("auth:logout-after:42")).thenReturn(Long.toString(now));

        assertThat(service.isLoggedOut(42L, Instant.ofEpochMilli(now - 1000))).isTrue();
    }

    @Test
    @DisplayName("isLoggedOut — 토큰 iat >= cutoff → false")
    void shouldReturnFalseWhenTokenAfterCutoff() {
        long now = System.currentTimeMillis();
        when(ops.get("auth:logout-after:42")).thenReturn(Long.toString(now));

        assertThat(service.isLoggedOut(42L, Instant.ofEpochMilli(now + 1000))).isFalse();
    }
}
