package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 해외주식 기간별시세 API 응답 DTO (TR_ID: HHDFS76240000).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisOverseasDailyResponse {

    @JsonProperty("rt_cd")
    private String resultCode;
    @JsonProperty("msg_cd")
    private String messageCode;
    @JsonProperty("msg1")
    private String message;
    @JsonProperty("output2")
    private List<KisOverseasDailyOutput> outputs;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
