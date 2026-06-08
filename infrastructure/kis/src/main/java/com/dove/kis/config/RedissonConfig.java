package com.dove.kis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KIS 분산 율제한(여러 인스턴스 공유)용 Redisson 클라이언트.
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        // 지연 초기화: 기동 시 즉시 연결하지 않고 첫 사용 시 연결 — Redis 없이도 컨텍스트 로드(테스트) 가능.
        config.setLazyInitialization(true);
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}
