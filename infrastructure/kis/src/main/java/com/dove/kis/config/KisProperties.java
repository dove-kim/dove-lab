package com.dove.kis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

/**
 * 한국투자증권 Open API 연결 설정 (app 모듈 application.yml의 kis.* 키).
 *
 * <pre>
 * kis:
 *   app-key: ${KIS_APP_KEY}
 *   app-secret: ${KIS_APP_SECRET}
 *   base-url: https://openapi.koreainvestment.com:9443   # 실전
 *   # base-url: https://openapivts.koreainvestment.com:29443  # 모의
 *   # data-start-date: 2020-01-01   # 기본값(1985-10-05)보다 최신으로 제한 가능
 * </pre>
 */
@ConfigurationProperties(prefix = "kis")
@Getter
@Setter
public class KisProperties {
    private String appKey;
    private String appSecret;
    private String baseUrl = "https://openapi.koreainvestment.com:9443";
    /** KIS 일봉 데이터 기산일. 미설정 시 KIS 시스템 최초 제공일(1985-10-05) 사용. */
    private LocalDate dataStartDate = LocalDate.of(1985, 10, 5);

    /** KIS 주식 API 초당 최대 시작 수. (20 = 50ms 간격) */
    private int stockPerSecond = 20;

    /** EGW00201(초당 한도) 발생 시 최대 재시도 횟수. throttle 지속 시간을 커버할 만큼 설정. */
    private int stockMaxRetries = 5;

    /** KIS 주식 API 최대 동시 호출 수 상한. */
    private int stockMaxConcurrent = 20;
}
