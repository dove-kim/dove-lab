package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 해외주식 현재체결가 API 응답 DTO (TR_ID: HHDFS00000300).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisOverseasPriceResponse {

    @JsonProperty("rt_cd")
    private String resultCode;
    @JsonProperty("msg_cd")
    private String messageCode;
    @JsonProperty("msg1")
    private String message;
    @JsonProperty("output")
    private KisOverseasPriceOutput output;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
