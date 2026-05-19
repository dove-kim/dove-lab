package com.dove.scheduler;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 통합 테스트용 Spring Boot 애플리케이션.
 * 스케줄은 application.yml에서 비활성화(cron = 2099년) 처리.
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
}
