package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 상품기본조회 응답의 output 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisProductInfoOutput {
    @JsonProperty("pdno")               private String pdno;
    @JsonProperty("prdt_type_cd")       private String prdtTypeCd;
    @JsonProperty("prdt_name")          private String prdtName;
    @JsonProperty("prdt_abrv_name")     private String prdtAbrvName;
    @JsonProperty("prdt_eng_name")      private String prdtEngName;
    @JsonProperty("shtn_pdno")          private String shtnPdno;
    @JsonProperty("prdt_risk_grad_cd")  private String prdtRiskGradCd;
    @JsonProperty("prdt_clsf_cd")       private String prdtClsfCd;
    @JsonProperty("prdt_clsf_name")     private String prdtClsfName;
    @JsonProperty("ivst_prdt_type_cd")  private String ivstPrdtTypeCd;
    @JsonProperty("ivst_prdt_type_cd_name") private String ivstPrdtTypeCdName;
    @JsonProperty("frst_erlm_dt")       private String frstErlmDt;
}
