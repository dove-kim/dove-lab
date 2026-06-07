package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 예탁원정보(KSD) 공통 응답 DTO. output1 배열을 필드 맵으로 받는다(유형별 필드가 달라 범용 처리).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisKsdResponse {

    @JsonProperty("rt_cd")
    private String returnCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output1")
    private List<Map<String, Object>> output1;

    // 일부 KSD API(예: 유상증자 paidin-capin)는 문서상 output 으로 응답 — 양쪽 수용.
    @JsonProperty("output")
    private List<Map<String, Object>> output;

    public boolean isSuccess() {
        return "0".equals(returnCode);
    }

    /** output1 우선, 없으면 output 반환 (API별 응답 키 차이 흡수). */
    public List<Map<String, Object>> rows() {
        if (output1 != null) return output1;
        return output;
    }
}
