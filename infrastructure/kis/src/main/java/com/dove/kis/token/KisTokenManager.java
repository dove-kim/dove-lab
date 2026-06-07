package com.dove.kis.token;

import com.dove.kis.config.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * KIS Access Token 캐시. 인메모리 + Redis 이중 저장, 만료 5분 전 자동 갱신.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenManager {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REDIS_KEY = "kis:access-token";
    /** Redis TTL을 실제 만료보다 5분 일찍 끊어 갱신 여유를 확보 */
    private static final long REFRESH_BUFFER_SECONDS = 300;

    private final KisTokenClient tokenClient;
    private final KisProperties properties;
    private final StringRedisTemplate redis;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public synchronized String getToken() {
        if (isMemoryValid()) return cachedToken;
        if (loadFromRedis()) return cachedToken;
        refresh();
        return cachedToken;
    }

    public synchronized void revokeToken() {
        if (cachedToken == null) return;
        log.info("KIS Access Token 폐기 요청");
        KisTokenRevokeResponse response = tokenClient.revokeToken(
                new KisTokenRevokeRequest(properties.getAppKey(), properties.getAppSecret(), cachedToken)
        );
        log.info("KIS Access Token 폐기 완료: code={}, message={}", response.getCode(), response.getMessage());
        cachedToken = null;
        tokenExpiry = Instant.EPOCH;
        deleteFromRedis();
    }

    private boolean isMemoryValid() {
        return cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(REFRESH_BUFFER_SECONDS));
    }

    /** Redis에서 토큰을 복원한다. 성공 시 true. */
    private boolean loadFromRedis() {
        try {
            String value = redis.opsForValue().get(REDIS_KEY);
            if (value == null) return false;
            // 포맷: "{token}|||{expiresAtEpochSecond}"
            int sep = value.lastIndexOf("|||");
            if (sep < 0) return false;
            String token = value.substring(0, sep);
            Instant expiry = Instant.ofEpochSecond(Long.parseLong(value.substring(sep + 3)));
            if (Instant.now().isAfter(expiry.minusSeconds(REFRESH_BUFFER_SECONDS))) return false;
            cachedToken = token;
            tokenExpiry = expiry;
            log.info("KIS Access Token Redis 복원, 만료={}", expiry.atZone(KST));
            return true;
        } catch (Exception e) {
            log.warn("KIS Access Token Redis 조회 실패: {}", e.getMessage());
            return false;
        }
    }

    private void refresh() {
        log.info("KIS Access Token 갱신 시작");
        KisTokenResponse response = tokenClient.getToken(
                new KisTokenRequest("client_credentials", properties.getAppKey(), properties.getAppSecret())
        );
        cachedToken = response.getAccessToken();
        tokenExpiry = parseExpiry(response);
        saveToRedis();
        log.info("KIS Access Token 갱신 완료, 만료={}", tokenExpiry.atZone(KST));
    }

    private void saveToRedis() {
        try {
            long ttl = tokenExpiry.getEpochSecond() - Instant.now().getEpochSecond() - REFRESH_BUFFER_SECONDS;
            if (ttl <= 0) return;
            String value = cachedToken + "|||" + tokenExpiry.getEpochSecond();
            redis.opsForValue().set(REDIS_KEY, value, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("KIS Access Token Redis 저장 실패: {}", e.getMessage());
        }
    }

    private void deleteFromRedis() {
        try {
            redis.delete(REDIS_KEY);
        } catch (Exception e) {
            log.warn("KIS Access Token Redis 삭제 실패: {}", e.getMessage());
        }
    }

    private Instant parseExpiry(KisTokenResponse response) {
        Instant expiry = null;
        String expiredStr = response.getAccessTokenExpired();
        if (expiredStr != null && !expiredStr.isBlank()) {
            try {
                expiry = LocalDateTime.parse(expiredStr.trim(), KST_FORMATTER)
                        .atZone(KST)
                        .toInstant();
            } catch (Exception ignored) { /* 파싱 실패 시 fallback */ }
        }
        if (expiry == null) {
            expiry = Instant.now().plusSeconds(response.getExpiresIn());
        }
        // KIS가 과거/짧은 만료 시간을 반환하면 24시간으로 재설정
        if (!expiry.isAfter(Instant.now().plusSeconds(REFRESH_BUFFER_SECONDS + 60))) {
            log.warn("KIS 토큰 만료 시간이 이미 지났거나 너무 짧음({}), 24시간으로 재설정", expiry.atZone(KST));
            expiry = Instant.now().plus(Duration.ofHours(24));
        }
        return expiry;
    }
}
