package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 국내휴장일조회(CTCA0903R) 응답.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisHolidayResponse {

    @JsonProperty("rt_cd")   private String resultCode;
    @JsonProperty("msg_cd")  private String messageCode;
    @JsonProperty("msg1")    private String message;
    @JsonProperty("output")  private List<KisHolidayItem> output;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /**
     * 국내휴장일조회 응답의 일자별 항목.
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KisHolidayItem {
        /** 영업일자 */
        @JsonProperty("bass_dt")   private String bassDate;
        /** 개장일여부 Y=개장, N=휴장 */
        @JsonProperty("opnd_yn")   private String openYn;
        /** 영업일여부 */
        @JsonProperty("bzdy_tp_cd") private String businessDayTypeCd;
        /** 거래일여부 */
        @JsonProperty("tr_day")    private String tradingDay;
        /** 결제일여부 */
        @JsonProperty("setl_day")  private String settlementDay;

        public boolean isOpen() {
            return "Y".equals(openYn);
        }
    }
}
