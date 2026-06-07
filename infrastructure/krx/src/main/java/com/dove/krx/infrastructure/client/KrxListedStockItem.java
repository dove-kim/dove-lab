package com.dove.krx.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * KRX 상장 종목 단건 원본 데이터.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrxListedStockItem {
    private static final DateTimeFormatter LIST_DD_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @JsonProperty("ISU_SRT_CD")
    private String ticker;

    @JsonProperty("ISU_CD")
    private String isin;

    @JsonProperty("ISU_NM")
    private String stockName;

    @JsonProperty("ISU_ABBRV")
    private String stockAbbrName;

    @JsonProperty("ISU_ENG_NM")
    private String stockEngName;

    /**
     * LIST_DD 원문 (yyyyMMdd 형식).
     */
    @JsonProperty("LIST_DD")
    private String listingDateStr;

    @JsonProperty("MKT_TP_NM")
    private String marketTypeName;

    /**
     * KRX 증권 그룹 원문 (SECUGRP_NM). 예: "주권", "ETF", "리츠".
     */
    @JsonProperty("SECUGRP_NM")
    private String secuGrpNm;

    /**
     * KRX 소속부 원문 (SECT_TP_NM). 예: "우량기업부", "벤처기업부".
     */
    @JsonProperty("SECT_TP_NM")
    private String sectorTypeName;

    /**
     * KRX 주권 종류 원문 (KIND_STKCERT_TP_NM). 주권 외 종목은 공백.
     */
    @JsonProperty("KIND_STKCERT_TP_NM")
    private String kindStkCertTpNm;

    @JsonProperty("PARVAL")
    private String parval;

    @JsonProperty("LIST_SHRS")
    private String listedShares;

    /**
     * LIST_DD 문자열을 LocalDate로 변환. 파싱 불가 시 null 반환.
     */
    public LocalDate getListingDate() {
        if (listingDateStr == null || listingDateStr.isBlank()) return null;
        try {
            return LocalDate.parse(listingDateStr.trim(), LIST_DD_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
