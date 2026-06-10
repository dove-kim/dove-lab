package com.dove.krx.config;

import com.dove.apiquota.RateLimiter;
import com.dove.apiquota.RedissonRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KRX API 초당 율제한 구성 (Redis 분산 rate limiter).
 */
@Configuration
public class KrxRateLimiterConfig {

    @Bean
    public RateLimiter krxRateLimiter(KrxProperties props, RedissonClient redisson) {
        return new RedissonRateLimiter(redisson, "krx:rate", props.getPerSecond());
    }
}