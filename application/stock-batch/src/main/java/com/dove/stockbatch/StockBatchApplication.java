package com.dove.stockbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class StockBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockBatchApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

}
