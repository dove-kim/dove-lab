package com.dove.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// @EnableScheduling은 SchedulingConfig(@Profile("!local"))로 분리 — local에선 cron 대신 LocalJobRunner의 수동 JOB만 실행.
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class SchedulerApplication {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

    /**
     * 시스템 시계. JOB_DATE(yyyy-MM-dd) 지정 시 그 날짜 정오(KST)로 고정 — 로컬 배치 테스트용.
     * 미지정(운영)이면 실제 시스템 시계를 사용한다.
     */
    @Bean
    public Clock clock(@Value("${JOB_DATE:}") String jobDate) {
        if (jobDate == null || jobDate.isBlank()) {
            return Clock.system(KST);
        }
        Instant fixed = LocalDate.parse(jobDate.trim()).atTime(12, 0).atZone(KST).toInstant();
        return Clock.fixed(fixed, KST);
    }
}
