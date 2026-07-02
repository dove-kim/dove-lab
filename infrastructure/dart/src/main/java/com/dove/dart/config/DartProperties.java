package com.dove.dart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DART OpenAPI 접속 설정.
 */
@ConfigurationProperties(prefix = "dart")
@Getter
@Setter
public class DartProperties {

    private String apiKey = "";
    private String baseUrl = "https://opendart.fss.or.kr/api";
    private int dailyQuota = 18000;
    private int maxConcurrent = 10;
}
