package com.dove.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정. 엔티티 스캔 및 리포지토리 활성화를 담당한다.
 */
@Configuration
@EntityScan(basePackages = "com.dove")
@EnableJpaRepositories(basePackages = "com.dove")
public class JpaConfig {
}
