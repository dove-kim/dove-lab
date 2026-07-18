package com.dove.frankfurter.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Frankfurter 인프라 모듈 Spring 설정.
 */
@Configuration
@EnableFeignClients(basePackages = "com.dove.frankfurter")
public class FrankfurterConfiguration {
}
