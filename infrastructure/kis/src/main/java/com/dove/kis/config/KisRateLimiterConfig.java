package com.dove.kis.config;

import com.dove.apiquota.RateLimiter;
import com.dove.apiquota.RedissonRateLimiter;
import com.dove.kis.quota.KisGate;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KIS 주식 API 게이트 구성 (Redis 분산 rate limiter + 동시 상한 세마포어).
 */
@Configuration
public class KisRateLimiterConfig {

    @Bean
    public KisGate stockGate(KisProperties props, RedissonClient redisson) {
        RateLimiter rateLimiter = new RedissonRateLimiter(redisson, "kis:stock:rate", props.getStockPerSecond());
        return new KisGate(rateLimiter, props.getStockMaxConcurrent(), props.getStockMaxRetries());
    }
}
