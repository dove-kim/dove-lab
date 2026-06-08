package com.dove.api;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootApplication(scanBasePackages = "com.dove")
@Import(TestApiApplication.TestInfraConfig.class)
public class TestApiApplication {

    /**
     * 외부 시스템(Redis) 의존 빈을 mock으로 교체한다.
     */
    @TestConfiguration
    static class TestInfraConfig {

        @Bean
        public RedissonClient redissonClient() {
            RedissonClient redisson = mock(RedissonClient.class);
            when(redisson.getRateLimiter(anyString())).thenReturn(mock(RRateLimiter.class));
            return redisson;
        }

        @SuppressWarnings("unchecked")
        @Bean
        public StringRedisTemplate stringRedisTemplate() {
            StringRedisTemplate template = mock(StringRedisTemplate.class);
            ValueOperations<String, String> valueOps = mock(ValueOperations.class);
            when(template.opsForValue()).thenReturn(valueOps);
            HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
            when(template.opsForHash()).thenReturn(hashOps);
            return template;
        }
    }
}
