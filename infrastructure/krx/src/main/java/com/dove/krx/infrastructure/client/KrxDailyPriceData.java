package com.dove.krx.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * KRX 일별 시세 단건 원본 데이터.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrxDailyPriceData {
    @JsonProperty("BAS_DD")
    private String baseDateStr;

    @JsonProperty("ISU_CD")
    private String stockCode;

    @JsonProperty("ISU_NM")
    private String stockName;

    @JsonProperty("MKT_NM")
    private String marketName;

    @JsonProperty("SECT_TP_NM")
    private String sectorType;

    @JsonProperty("TDD_CLSPRC")
    private String closingPriceStr;

    @JsonProperty("CMPPREVDD_PRC")
    private String priceChangeStr;

    @JsonProperty("FLUC_RT")
    private String fluctuationRateStr;

    @JsonProperty("TDD_OPNPRC")
    private String openingPriceStr;

    @JsonProperty("TDD_HGPRC")
    private String highPriceStr;

    @JsonProperty("TDD_LWPRC")
    private String lowPriceStr;

    @JsonProperty("ACC_TRDVOL")
    private String tradingVolumeStr;

    @JsonProperty("ACC_TRDVAL")
    private String tradingValueStr;

    @JsonProperty("MKTCAP")
    private String marketCapStr;

    @JsonProperty("LIST_SHRS")
    private String listedSharesStr;

    public LocalDate getBaseDate() {
        return LocalDate.parse(baseDateStr, DateTimeFormatter.BASIC_ISO_DATE);
    }

    public Long getClosingPrice() {
        return Long.parseLong(closingPriceStr);
    }

    public Long getPriceChange() {
        return Long.parseLong(priceChangeStr);
    }

    public Long getOpeningPrice() {
        return Long.parseLong(openingPriceStr);
    }

    public Long getHighPrice() {
        return Long.parseLong(highPriceStr);
    }

    public Long getLowPrice() {
        return Long.parseLong(lowPriceStr);
    }

    public Long getTradingVolume() {
        return Long.parseLong(tradingVolumeStr);
    }

    public Long getTradingValue() {
        return Long.parseLong(tradingValueStr);
    }

    public Long getMarketCap() {
        return Long.parseLong(marketCapStr);
    }

    public Long getListedShares() {
        return Long.parseLong(listedSharesStr);
    }

    /**
     * 원본 필드를 KRX 응답과 동일한 JSON 문자열로 직렬화한다.
     */
    public String toJson() {
        return String.format(
                "{\"BAS_DD\":\"%s\"," +
                        "\"ISU_CD\":\"%s\"," +
                        "\"ISU_NM\":\"%s\"," +
                        "\"MKT_NM\":\"%s\"," +
                        "\"SECT_TP_NM\":\"%s\"," +
                        "\"TDD_CLSPRC\":\"%s\"," +
                        "\"CMPPREVDD_PRC\":\"%s\"," +
                        "\"FLUC_RT\":\"%s\"," +
                        "\"TDD_OPNPRC\":\"%s\"," +
                        "\"TDD_HGPRC\":\"%s\"," +
                        "\"TDD_LWPRC\":\"%s\"," +
                        "\"ACC_TRDVOL\":\"%s\"," +
                        "\"ACC_TRDVAL\":\"%s\"," +
                        "\"MKTCAP\":\"%s\"," +
                        "\"LIST_SHRS\":\"%s\"}",
                baseDateStr, stockCode, stockName, marketName, sectorType,
                closingPriceStr, priceChangeStr, fluctuationRateStr, openingPriceStr,
                highPriceStr, lowPriceStr, tradingVolumeStr, tradingValueStr,
                marketCapStr, listedSharesStr
        );
    }
}
