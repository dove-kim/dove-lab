package com.dove.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 권한 변경·로그아웃 시 특정 시각 이전 발급된 access token을 모두 무효화한다.
 */
@Service
@RequiredArgsConstructor
public class ForcedLogoutService {

    private static final String KEY_PREFIX = "auth:logout-after:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    /**
     * 권한 변경·로그아웃 시 호출: 이 시점 이전 발급 토큰 모두 무효화.
     */
    public void markLogoutNow(Long memberId) {
        long now = System.currentTimeMillis();
        redisTemplate.opsForValue().set(
                key(memberId), Long.toString(now), Duration.ofMillis(refreshExpirationMs));
    }

    /**
     * 토큰 issuedAt이 cutoff 이전이면 true.
     */
    public boolean isLoggedOut(Long memberId, Instant tokenIssuedAt) {
        if (memberId == null || tokenIssuedAt == null) return false;
        String raw = redisTemplate.opsForValue().get(key(memberId));
        if (raw == null) return false;
        try {
            // JWT iat는 초 단위라 밀리초 비교 시 같은 초 재발급 토큰까지 무효화됨 → 초 단위로 비교
            long cutoffSecond = Long.parseLong(raw) / 1000L;
            return tokenIssuedAt.getEpochSecond() < cutoffSecond;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
