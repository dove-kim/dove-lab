package com.dove.krx.quota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Redis 기반 일일 API 호출 한도 카운터.
 */
@Slf4j
public class DailyApiQuota {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redis;
    private final String countKeyPrefix;
    private final String limitKey;
    private final String lastLimitKey;
    private final int defaultLimit;

    public DailyApiQuota(StringRedisTemplate redis, String namespace, int defaultLimit) {
        this.redis = redis;
        this.countKeyPrefix = namespace + ":daily-count:";
        this.limitKey = namespace + ":daily-limit";
        this.lastLimitKey = namespace + ":last-rate-limit-at";
        this.defaultLimit = defaultLimit;
    }

    /**
     * 기동 시 Redis 한도값을 설정값으로 동기화한다.
     */
    public void ensureLimit(int configuredLimit, String logLabel) {
        String stored = redis.opsForValue().get(limitKey);
        if (stored == null) {
            redis.opsForValue().set(limitKey, String.valueOf(configuredLimit));
            log.info("[{}] 일일 한도 Redis 초기화: {}", logLabel, configuredLimit);
        } else if (!stored.equals(String.valueOf(configuredLimit))) {
            redis.opsForValue().set(limitKey, String.valueOf(configuredLimit));
            log.info("[{}] 일일 한도 변경 적용: {} → {}", logLabel, stored, configuredLimit);
        }
    }

    /**
     * 카운트를 증가시키고 한도 내 호출 가능 여부를 반환한다.
     *
     * @return 호출 가능하면 true, 한도 초과면 false
     */
    public boolean tryAcquire() {
        String key = countKeyForToday();
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redis.expire(key, Duration.ofHours(26));
        }
        int limit = currentLimit();
        if (current == null || current > limit) {
            redis.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    /**
     * 서버가 한도 초과 응답을 보낼 때 카운트를 한도까지 채운다.
     */
    public void markRemoteLimited() {
        String key = countKeyForToday();
        redis.opsForValue().set(key, String.valueOf(currentLimit()));
        redis.expire(key, Duration.ofHours(26));
        redis.opsForValue().set(lastLimitKey,
                ZonedDateTime.now(KST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * 오늘 누적 호출 횟수를 반환한다. Redis에 값이 없으면 0을 반환한다.
     */
    public int currentCount() {
        String v = redis.opsForValue().get(countKeyForToday());
        return v == null ? 0 : Integer.parseInt(v);
    }

    /**
     * 설정된 일일 한도를 반환한다. Redis에 값이 없으면 기본값을 반환한다.
     */
    public int currentLimit() {
        String v = redis.opsForValue().get(limitKey);
        return v == null ? defaultLimit : Integer.parseInt(v);
    }

    /**
     * 마지막으로 한도 초과가 발생한 시각을 반환한다. 기록이 없으면 empty를 반환한다.
     */
    public Optional<ZonedDateTime> lastLimitAt() {
        String v = redis.opsForValue().get(lastLimitKey);
        return v == null ? Optional.empty()
                : Optional.of(ZonedDateTime.parse(v, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    private String countKeyForToday() {
        return countKeyPrefix + LocalDate.now(KST).toString();
    }
}
