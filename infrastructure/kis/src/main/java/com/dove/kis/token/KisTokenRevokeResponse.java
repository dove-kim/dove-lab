package com.dove.kis.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 접근토큰 폐기 응답.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisTokenRevokeResponse {
    @JsonProperty("code")
    private int code;
    @JsonProperty("message")
    private String message;
}
