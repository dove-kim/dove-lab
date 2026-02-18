package com.dove.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정
 *
 * @see QuerydslConfiguration
 */
@Configuration
@EntityScan(basePackages = "com.dove")
@EnableJpaRepositories(basePackages = "com.dove")
public class JpaConfig {
}
