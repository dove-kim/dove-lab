package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 국내주식 종목추정실적 응답 (TR_ID: HHKST668300C0).
 * output1=종목/애널/투자의견, output2=추정손익, output3=투자지표, output4=결산월.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisEstimateResponse {

    @JsonProperty("rt_cd")
    private String returnCode;

    @JsonProperty("output1")
    private Map<String, Object> output1;

    @JsonProperty("output2")
    private List<Map<String, Object>> output2;

    @JsonProperty("output3")
    private List<Map<String, Object>> output3;

    @JsonProperty("output4")
    private List<Map<String, Object>> output4;

    public boolean isSuccess() {
        return "0".equals(returnCode);
    }
}
