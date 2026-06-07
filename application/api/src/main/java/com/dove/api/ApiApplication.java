package com.dove.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Clock;
import java.time.ZoneId;

/**
 * REST API 서버 실행 진입점.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    /**
     * Asia/Seoul 기준 시스템 Clock 빈을 제공한다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
