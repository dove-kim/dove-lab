package com.dove.apiquota;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 모든 인스턴스가 슬롯을 공유하는 Redisson 기반 분산 rate limiter.
 */
public class RedissonRateLimiter implements RateLimiter {

    private final RRateLimiter rateLimiter;

    public RedissonRateLimiter(RedissonClient redisson, String key, int maxPerSecond) {
        this.rateLimiter = redisson.getRateLimiter(key);
        // 버스트 방지: "1초당 N건"이 아니라 "intervalMs당 1건"으로 균등 분산
        long intervalMs = Math.max(1, 1000L / maxPerSecond);
        this.rateLimiter.trySetRate(RateType.OVERALL, 1, intervalMs, RateIntervalUnit.MILLISECONDS);
    }

    @Override
    public boolean tryAcquire(Duration timeout) throws InterruptedException {
        return rateLimiter.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
