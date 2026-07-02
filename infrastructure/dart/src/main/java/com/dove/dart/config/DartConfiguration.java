package com.dove.dart.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * DART 인프라 모듈 Spring 설정.
 */
@Configuration
@EnableFeignClients(basePackages = "com.dove.dart")
@EnableConfigurationProperties(DartProperties.class)
public class DartConfiguration {
}
