package com.dove.distributedlock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DistributedLockProperties.class)
public class DistributedLockConfig {
}
