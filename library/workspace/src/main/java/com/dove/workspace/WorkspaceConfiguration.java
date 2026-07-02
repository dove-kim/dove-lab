package com.dove.workspace;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 작업 디렉터리 라이브러리 Spring 설정.
 */
@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfiguration {
}
