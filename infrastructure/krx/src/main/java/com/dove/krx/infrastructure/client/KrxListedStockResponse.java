package com.dove.krx.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** KRX 상장 종목 조회 응답 DTO. 티커(ISU_SRT_CD)·ISIN(ISU_CD)·종목명(ISU_NM)·상장일(LIST_DD) 매핑. */
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
        private static final DateTimeFormatter LIST_DD_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

        @JsonProperty("ISU_SRT_CD")
        private String ticker;

        @JsonProperty("ISU_CD")
        private String isin;

        @JsonProperty("ISU_NM")
        private String stockName;

        /** LIST_DD 원문 (yyyyMMdd 형식). */
        @JsonProperty("LIST_DD")
        private String listingDateStr;

        /** LIST_DD 문자열을 LocalDate로 변환. 파싱 불가 시 null 반환. */
        public LocalDate getListingDate() {
            if (listingDateStr == null || listingDateStr.isBlank()) return null;
            try {
                return LocalDate.parse(listingDateStr.trim(), LIST_DD_FMT);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
