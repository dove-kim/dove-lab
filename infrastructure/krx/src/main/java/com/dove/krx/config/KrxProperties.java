package com.dove.krx.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KRX Open API 연결 설정 (krx.api.* 키).
 */
@ConfigurationProperties(prefix = "krx.api")
@Getter
@Setter
public class KrxProperties {

    /** KRX Open API 인증 키. */
    private String authKey = "";

    /** KRX API 일일 호출 한도. */
    private int dailyQuota = 6000;

    /** 초당 요청 수. 실측 안전치 ~20/sec 기준 15. */
    private int perSecond = 15;
}
