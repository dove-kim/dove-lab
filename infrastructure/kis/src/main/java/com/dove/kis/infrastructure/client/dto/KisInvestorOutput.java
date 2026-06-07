package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 종목별 투자자매매동향 응답의 일자별 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisInvestorOutput {

    /** 영업 일자 (yyyyMMdd) */
    @JsonProperty("stck_bsop_date")   private String tradingDate;

    /** 개인 매수 수량 */
    @JsonProperty("prsn_shnu_vol")    private String individualBuyVol;
    /** 개인 매도 수량 */
    @JsonProperty("prsn_seln_vol")    private String individualSellVol;
    /** 개인 순매수 수량 */
    @JsonProperty("prsn_ntby_vol")    private String individualNetBuyVol;
    /** 개인 매수 금액 */
    @JsonProperty("prsn_shnu_amt")    private String individualBuyAmt;
    /** 개인 매도 금액 */
    @JsonProperty("prsn_seln_amt")    private String individualSellAmt;
    /** 개인 순매수 금액 */
    @JsonProperty("prsn_ntby_amt")    private String individualNetBuyAmt;

    /** 기관 매수 수량 */
    @JsonProperty("orgn_shnu_vol")    private String institutionBuyVol;
    /** 기관 매도 수량 */
    @JsonProperty("orgn_seln_vol")    private String institutionSellVol;
    /** 기관 순매수 수량 */
    @JsonProperty("orgn_ntby_vol")    private String institutionNetBuyVol;
    /** 기관 매수 금액 */
    @JsonProperty("orgn_shnu_amt")    private String institutionBuyAmt;
    /** 기관 매도 금액 */
    @JsonProperty("orgn_seln_amt")    private String institutionSellAmt;
    /** 기관 순매수 금액 */
    @JsonProperty("orgn_ntby_amt")    private String institutionNetBuyAmt;

    /** 외국인 매수 수량 */
    @JsonProperty("frgn_shnu_vol")    private String foreignBuyVol;
    /** 외국인 매도 수량 */
    @JsonProperty("frgn_seln_vol")    private String foreignSellVol;
    /** 외국인 순매수 수량 */
    @JsonProperty("frgn_ntby_vol")    private String foreignNetBuyVol;
    /** 외국인 매수 금액 */
    @JsonProperty("frgn_shnu_amt")    private String foreignBuyAmt;
    /** 외국인 매도 금액 */
    @JsonProperty("frgn_seln_amt")    private String foreignSellAmt;
    /** 외국인 순매수 금액 */
    @JsonProperty("frgn_ntby_amt")    private String foreignNetBuyAmt;

    /** 기타법인 매수 수량 */
    @JsonProperty("ctfc_shnu_vol")    private String otherCorpBuyVol;
    /** 기타법인 매도 수량 */
    @JsonProperty("ctfc_seln_vol")    private String otherCorpSellVol;
    /** 기타법인 순매수 수량 */
    @JsonProperty("ctfc_ntby_vol")    private String otherCorpNetBuyVol;
    /** 기타법인 매수 금액 */
    @JsonProperty("ctfc_shnu_amt")    private String otherCorpBuyAmt;
    /** 기타법인 매도 금액 */
    @JsonProperty("ctfc_seln_amt")    private String otherCorpSellAmt;
    /** 기타법인 순매수 금액 */
    @JsonProperty("ctfc_ntby_amt")    private String otherCorpNetBuyAmt;

    public long getIndividualBuyVolLong()      { return parseLong(individualBuyVol); }
    public long getIndividualSellVolLong()     { return parseLong(individualSellVol); }
    public long getIndividualNetBuyVolLong()   { return parseLong(individualNetBuyVol); }
    public long getIndividualBuyAmtLong()      { return parseLong(individualBuyAmt); }
    public long getIndividualSellAmtLong()     { return parseLong(individualSellAmt); }
    public long getIndividualNetBuyAmtLong()   { return parseLong(individualNetBuyAmt); }

    public long getInstitutionBuyVolLong()     { return parseLong(institutionBuyVol); }
    public long getInstitutionSellVolLong()    { return parseLong(institutionSellVol); }
    public long getInstitutionNetBuyVolLong()  { return parseLong(institutionNetBuyVol); }
    public long getInstitutionBuyAmtLong()     { return parseLong(institutionBuyAmt); }
    public long getInstitutionSellAmtLong()    { return parseLong(institutionSellAmt); }
    public long getInstitutionNetBuyAmtLong()  { return parseLong(institutionNetBuyAmt); }

    public long getForeignBuyVolLong()         { return parseLong(foreignBuyVol); }
    public long getForeignSellVolLong()        { return parseLong(foreignSellVol); }
    public long getForeignNetBuyVolLong()      { return parseLong(foreignNetBuyVol); }
    public long getForeignBuyAmtLong()         { return parseLong(foreignBuyAmt); }
    public long getForeignSellAmtLong()        { return parseLong(foreignSellAmt); }
    public long getForeignNetBuyAmtLong()      { return parseLong(foreignNetBuyAmt); }

    public long getOtherCorpBuyVolLong()       { return parseLong(otherCorpBuyVol); }
    public long getOtherCorpSellVolLong()      { return parseLong(otherCorpSellVol); }
    public long getOtherCorpNetBuyVolLong()    { return parseLong(otherCorpNetBuyVol); }
    public long getOtherCorpBuyAmtLong()       { return parseLong(otherCorpBuyAmt); }
    public long getOtherCorpSellAmtLong()      { return parseLong(otherCorpSellAmt); }
    public long getOtherCorpNetBuyAmtLong()    { return parseLong(otherCorpNetBuyAmt); }

    private static long parseLong(String v) {
        if (v == null || v.isBlank()) return 0L;
        try {
            return Long.parseLong(v.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
