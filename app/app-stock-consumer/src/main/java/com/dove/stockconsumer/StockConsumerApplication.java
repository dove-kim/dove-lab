package com.dove.stockconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 주식 컨슈머 애플리케이션.
 * Kafka 토픽을 구독하여 KRX 주가 저장 및 기술적 지표 계산을 처리한다.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class StockConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockConsumerApplication.class, args);
	}
}
