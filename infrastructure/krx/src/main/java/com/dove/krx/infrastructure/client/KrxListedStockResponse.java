package com.dove.krx.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** KRX 상장 종목 조회 응답 DTO. ISU_CD, ISU_NM만 매핑. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrxListedStockResponse {
    @JsonProperty("OutBlock_1")
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("ISU_CD")
        private String stockCode;

        @JsonProperty("ISU_NM")
        private String stockName;
    }
}
