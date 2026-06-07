package com.dove.kis.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * KIS 인프라 모듈 Spring 설정.
 */
@Configuration
@EnableFeignClients(basePackages = "com.dove.kis")
@EnableConfigurationProperties(KisProperties.class)
public class KisConfiguration {
}
