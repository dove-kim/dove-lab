package com.dove.scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled cron 활성화 — 운영(non-local)에서만.
 */
@Configuration
@Profile("!local")
@EnableScheduling
public class SchedulingConfig {
}
