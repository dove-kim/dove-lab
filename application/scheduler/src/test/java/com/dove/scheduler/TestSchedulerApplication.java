package com.dove.scheduler;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 통합 테스트용 Spring Boot 애플리케이션.
 * 스케줄은 application.yml에서 비활성화(cron = 2099년) 처리.
 * Redis는 자동설정 제외 후 Mock으로 대체 (KrxApiQuotaService의 @PostConstruct 호환).
 */
@SpringBootApplication(scanBasePackages = "com.dove")
@EnableScheduling
public class TestSchedulerApplication {

    @Bean
    public Clock clock() {
        return Clock.fixed(
                LocalDate.of(2026, 4, 22).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class, Answers.RETURNS_DEEP_STUBS);
    }
}
