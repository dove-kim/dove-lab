package com.dove.krx.infrastructure;

import com.dove.krx.config.KrxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * KRX 인프라 설정. Feign 클라이언트 스캔을 활성화한다.
 */
@Configuration
@EnableFeignClients(basePackages = "com.dove.krx")
@EnableConfigurationProperties(KrxProperties.class)
public class KrxConfiguration {
}
