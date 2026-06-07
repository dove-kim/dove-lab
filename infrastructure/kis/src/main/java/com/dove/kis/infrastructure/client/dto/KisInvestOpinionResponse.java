package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 국내주식 종목투자의견 응답 (TR_ID: FHKST663300C0). */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisInvestOpinionResponse {

    @JsonProperty("rt_cd")
    private String returnCode;

    @JsonProperty("output")
    private List<Map<String, Object>> output;

    public boolean isSuccess() {
        return "0".equals(returnCode);
    }
}
