package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상품기본조회 API 응답 DTO (TR_ID: CTPF1604R). */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisProductInfoResponse {

    @JsonProperty("rt_cd") private String resultCode;
    @JsonProperty("msg_cd") private String messageCode;
    @JsonProperty("msg1")   private String message;
    @JsonProperty("output") private KisProductInfoOutput output;

    public boolean isSuccess() { return "0".equals(resultCode); }

}
