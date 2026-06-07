package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 주식현재가 시세 응답의 output 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisCurrentPriceOutput {
    @JsonProperty("stck_shrn_iscd")
    private String stockCode;
    @JsonProperty("rprs_mrkt_kor_name")
    private String marketName;
    @JsonProperty("bstp_kor_isnm")
    private String sectorName;
    @JsonProperty("stck_prpr")
    private String currentPrice;
    @JsonProperty("prdy_vrss")
    private String priceChange;
    @JsonProperty("prdy_vrss_sign")
    private String priceChangeSign;
    @JsonProperty("prdy_ctrt")
    private String priceChangeRate;
    @JsonProperty("stck_oprc")
    private String openPrice;
    @JsonProperty("stck_hgpr")
    private String highPrice;
    @JsonProperty("stck_lwpr")
    private String lowPrice;
    @JsonProperty("stck_mxpr")
    private String upperLimitPrice;
    @JsonProperty("stck_llam")
    private String lowerLimitPrice;
    @JsonProperty("stck_sdpr")
    private String basePrice;
    @JsonProperty("acml_vol")
    private String accumulatedVolume;
    @JsonProperty("acml_tr_pbmn")
    private String accumulatedTurnover;
    @JsonProperty("hts_avls")
    private String marketCap;
    @JsonProperty("lstn_stcn")
    private String listedShares;
    @JsonProperty("per")
    private String per;
    @JsonProperty("pbr")
    private String pbr;
    @JsonProperty("eps")
    private String eps;
    @JsonProperty("bps")
    private String bps;

    public long getCurrentPriceLong()        { return parseLong(currentPrice); }
    public long getPriceChangeLong()          { return parseLong(priceChange); }
    public double getPriceChangeRateDouble()  { return parseDouble(priceChangeRate); }
    public long getOpenPriceLong()            { return parseLong(openPrice); }
    public long getHighPriceLong()            { return parseLong(highPrice); }
    public long getLowPriceLong()             { return parseLong(lowPrice); }
    public long getUpperLimitPriceLong()      { return parseLong(upperLimitPrice); }
    public long getLowerLimitPriceLong()      { return parseLong(lowerLimitPrice); }
    public long getBasePriceLong()            { return parseLong(basePrice); }
    public long getAccumulatedVolumeLong()    { return parseLong(accumulatedVolume); }
    public long getAccumulatedTurnoverLong()  { return parseLong(accumulatedTurnover); }
    public long getMarketCapLong()            { return parseLong(marketCap); }
    public long getListedSharesLong()         { return parseLong(listedShares); }
    public double getPerDouble()              { return parseDouble(per); }
    public double getPbrDouble()              { return parseDouble(pbr); }
    public double getEpsDouble()              { return parseDouble(eps); }
    public double getBpsDouble()              { return parseDouble(bps); }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        return Long.parseLong(value.trim().replace(",", ""));
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        return Double.parseDouble(value.trim());
    }
}
