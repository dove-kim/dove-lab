package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 국내주식기간별시세 응답의 기간별 봉 항목(output2).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisPeriodChartBar {
    /** 영업 일자 yyyyMMdd */
    @JsonProperty("stck_bsop_date")
    private String tradingDate;
    @JsonProperty("stck_clpr")
    private String closePrice;
    @JsonProperty("stck_oprc")
    private String openPrice;
    @JsonProperty("stck_hgpr")
    private String highPrice;
    @JsonProperty("stck_lwpr")
    private String lowPrice;
    @JsonProperty("acml_vol")
    private String accumulatedVolume;
    @JsonProperty("acml_tr_pbmn")
    private String accumulatedTurnover;
    /** 락 구분 코드: 00=해당없음 01=권리락 02=배당락 03=분배락 04=권배락 등 */
    @JsonProperty("flng_cls_code")
    private String changeCode;
    @JsonProperty("prdy_vrss_sign")
    private String priceChangeSign;
    @JsonProperty("prdy_vrss")
    private String priceChange;

    public long getClosePriceLong()          { return parseLong(closePrice); }
    public long getOpenPriceLong()           { return parseLong(openPrice); }
    public long getHighPriceLong()           { return parseLong(highPrice); }
    public long getLowPriceLong()            { return parseLong(lowPrice); }
    public long getAccumulatedVolumeLong()   { return parseLong(accumulatedVolume); }
    public long getAccumulatedTurnoverLong() { return parseLong(accumulatedTurnover); }
    public long getPriceChangeLong()         { return parseLong(priceChange); }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        return Long.parseLong(value.trim().replace(",", ""));
    }

    /** 도메인 봉 타입으로 변환한다. */
    public KisDailyCandle toCandle() {
        return new KisDailyCandle(
                LocalDate.parse(tradingDate, DateTimeFormatter.BASIC_ISO_DATE),
                getOpenPriceLong(), getHighPriceLong(), getLowPriceLong(), getClosePriceLong(),
                getAccumulatedVolumeLong(), getAccumulatedTurnoverLong(),
                changeCode, priceChangeSign, getPriceChangeLong());
    }
}
