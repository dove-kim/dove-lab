package com.dove.kis.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * KIS 접근토큰 폐기 요청 바디.
 */
@Getter
@AllArgsConstructor
public class KisTokenRevokeRequest {
    @JsonProperty("appkey")
    private final String appKey;
    @JsonProperty("appsecret")
    private final String appSecret;
    @JsonProperty("token")
    private final String token;
}
