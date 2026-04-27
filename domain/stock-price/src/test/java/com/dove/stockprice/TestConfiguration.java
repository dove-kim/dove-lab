package com.dove.stockprice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.dove")
@EntityScan(basePackages = "com.dove")
@EnableJpaRepositories(basePackages = "com.dove")
public class TestConfiguration {
}
