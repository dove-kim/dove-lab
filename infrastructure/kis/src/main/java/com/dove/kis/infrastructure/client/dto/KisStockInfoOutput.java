package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 주식기본조회 응답의 output 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisStockInfoOutput {
    @JsonProperty("pdno")               private String stockCode;
    @JsonProperty("prdt_type_cd")       private String productTypeCd;
    @JsonProperty("prdt_name")          private String productName;
    @JsonProperty("prdt_abrv_name")     private String stockName;
    @JsonProperty("prdt_eng_name")      private String productEngName;
    @JsonProperty("shtn_pdno")          private String shortCode;
    @JsonProperty("mket_id_cd")         private String marketId;
    @JsonProperty("lstg_stqt")          private String lstgStqt;           // 상장주식수
    @JsonProperty("lstg_cptl_amt")      private String lstgCptlAmt;        // 상장자본금액
    @JsonProperty("papr")               private String papr;               // 액면가
    @JsonProperty("stck_kind_cd")       private String stckKindCd;         // 주식종류코드
    @JsonProperty("etf_dvsn_cd")        private String etfDvsnCd;
    @JsonProperty("reits_kind_cd")      private String reitsKindCd;
    @JsonProperty("kospi200_item_yn")   private String kospi200ItemYn;
    @JsonProperty("idx_bztp_lcls_cd")   private String idxBztpLclsCd;
    @JsonProperty("idx_bztp_mcls_cd")   private String idxBztpMclsCd;
    @JsonProperty("idx_bztp_scls_cd")   private String idxBztpSclsCd;
    @JsonProperty("idx_bztp_lcls_cd_name") private String idxBztpLclsNm;
    @JsonProperty("idx_bztp_mcls_cd_name") private String idxBztpMclsNm;
    @JsonProperty("idx_bztp_scls_cd_name") private String idxBztpSclsNm;
    @JsonProperty("std_idst_clsf_cd")   private String stdIdstClsfCd;
    @JsonProperty("std_idst_clsf_cd_name") private String stdIdstClsfNm;
    @JsonProperty("frnr_psnl_lmt_rt")   private String frnrPsnlLmtRt;
    @JsonProperty("tr_stop_yn")         private String trStopYn;
    @JsonProperty("admn_item_yn")       private String admnItemYn;
    @JsonProperty("lstg_abol_dt")       private String lstgAbolDt;
    @JsonProperty("scts_mket_lstg_dt")  private String sctsMketLstgDt;
    // 상장폐지 판단용 (기존 호환)
    @JsonProperty("delist_date")        private String delistDate;

    public long toLong(String v) {
        if (v == null || v.isBlank()) return 0L;
        try { return Long.parseLong(v.replace(",", "").trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    public Long getListedShares() { return toLong(lstgStqt); }
    public Long getCapitalAmount() { return toLong(lstgCptlAmt); }
    public Long getFaceValue() { return toLong(papr); }
    public boolean isDelisted() { return delistDate != null && !delistDate.isBlank() && !"00000000".equals(delistDate); }
}
