package com.dove.stockconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class StockConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockConsumerApplication.class, args);
	}
}
