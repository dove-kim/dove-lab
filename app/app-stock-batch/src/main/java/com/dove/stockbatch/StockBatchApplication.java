package com.dove.stockbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주식 배치 애플리케이션.
 * 스케줄링 기반으로 KRX 주가 조회 요청 발행 및 기술적 지표 계산 트리거를 수행한다.
 */
@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class StockBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockBatchApplication.class, args);
    }

}
