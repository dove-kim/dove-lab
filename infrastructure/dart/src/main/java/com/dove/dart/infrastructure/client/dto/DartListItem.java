package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DART 공시검색(list) 항목.
 *
 * @param corpCode  고유번호
 * @param stockCode 종목코드(비상장은 공란)
 * @param reportNm  보고서명([기재정정] 접두어로 정정 식별)
 * @param rceptNo   접수번호
 * @param rceptDt   접수일자(YYYYMMDD)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DartListItem(
        @JsonProperty("corp_code") String corpCode,
        @JsonProperty("stock_code") String stockCode,
        @JsonProperty("report_nm") String reportNm,
        @JsonProperty("rcept_no") String rceptNo,
        @JsonProperty("rcept_dt") String rceptDt
) {
}
