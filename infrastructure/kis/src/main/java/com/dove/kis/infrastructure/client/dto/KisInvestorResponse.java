package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 종목별 투자자매매동향 API 응답 DTO (TR_ID: FHKST01010900).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisInvestorResponse {

    @JsonProperty("rt_cd")
    private String returnCode;

    @JsonProperty("msg_cd")
    private String messageCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output")
    private List<KisInvestorOutput> dataList;

    public boolean isSuccess() {
        return "0".equals(returnCode);
    }

}
