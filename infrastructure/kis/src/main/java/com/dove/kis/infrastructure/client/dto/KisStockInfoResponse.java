package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 주식기본조회 API 응답 DTO (TR_ID: CTPF1002R). */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisStockInfoResponse {

    @JsonProperty("rt_cd")  private String returnCode;
    @JsonProperty("msg_cd") private String messageCode;
    @JsonProperty("msg1")   private String message;
    @JsonProperty("output") private KisStockInfoOutput output;

    public boolean isSuccess() { return "0".equals(returnCode); }

}
