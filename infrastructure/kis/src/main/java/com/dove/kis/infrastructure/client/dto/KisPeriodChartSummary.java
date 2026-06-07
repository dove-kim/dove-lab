package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 국내주식기간별시세 응답의 현재 시세 요약(output1).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisPeriodChartSummary {
    @JsonProperty("stck_shrn_iscd")
    private String stockCode;
    @JsonProperty("hts_kor_isnm")
    private String stockName;
    @JsonProperty("stck_prpr")
    private String currentPrice;
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
}
