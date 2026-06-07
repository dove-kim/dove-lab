package com.dove.kis.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * KIS 접근토큰 발급 요청 바디.
 */
@Getter
@AllArgsConstructor
public class KisTokenRequest {
    @JsonProperty("grant_type")
    private final String grantType;
    @JsonProperty("appkey")
    private final String appKey;
    @JsonProperty("appsecret")
    private final String appSecret;
}
