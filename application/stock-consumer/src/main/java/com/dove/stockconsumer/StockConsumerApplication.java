package com.dove.stockconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Clock;
import java.time.ZoneId;

/** 주식 컨슈머 애플리케이션. Kafka 구독 기반 처리. */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class StockConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockConsumerApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
