package com.dove.krx.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * KRX 일별 시세 조회 응답 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrxDailyPriceResponse {
    @JsonProperty("OutBlock_1")
    private List<KrxDailyPriceData> dataList;

    /**
     * 응답 전체를 KRX 원본과 동일한 JSON 문자열로 직렬화한다.
     */
    public String toJson() {
        if (dataList == null || dataList.isEmpty()) {
            return "[]";
        }

        return "{\"OutBlock_1\":[" +
                dataList.stream()
                        .map(KrxDailyPriceData::toJson)
                        .collect(Collectors.joining(",")) +
                "]}";
    }
}
