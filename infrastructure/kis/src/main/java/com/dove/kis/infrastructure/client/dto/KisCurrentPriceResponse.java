package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주식현재가 시세 API 응답 DTO (TR_ID: FHKST01010100).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisCurrentPriceResponse {

    @JsonProperty("rt_cd")
    private String resultCode;
    @JsonProperty("msg_cd")
    private String messageCode;
    @JsonProperty("msg1")
    private String message;
    @JsonProperty("output")
    private KisCurrentPriceOutput output;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

}
