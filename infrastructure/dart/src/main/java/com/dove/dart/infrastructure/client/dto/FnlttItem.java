package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DART 전체 재무제표(fnlttSinglAcntAll) 계정 항목.
 *
 * @param rceptNo       접수번호
 * @param sjDiv         재무제표 구분(BS/IS/CIS/CF/SCE)
 * @param accountId     표준계정코드(ifrs-full_*, dart_*; 비표준은 '-표준계정코드 미사용-')
 * @param accountNm     계정명
 * @param thstrmAmount  당기금액(문자열, 콤마 포함 가능)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FnlttItem(
        @JsonProperty("rcept_no") String rceptNo,
        @JsonProperty("sj_div") String sjDiv,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("account_nm") String accountNm,
        @JsonProperty("thstrm_amount") String thstrmAmount
) {
}
