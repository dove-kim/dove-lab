package com.dove.kis.infrastructure.client.dto;

import com.dove.kis.KisDailyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 국내주식기간별시세 API 응답 DTO (TR_ID: FHKST03010100).
 * output1: 현재 시세 요약, output2: 기간별 봉 배열 (최대 100건)
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisPeriodChartResponse {

    @JsonProperty("rt_cd")
    private String resultCode;
    @JsonProperty("msg_cd")
    private String messageCode;
    @JsonProperty("msg1")
    private String message;
    @JsonProperty("output1")
    private KisPeriodChartSummary output1;
    @JsonProperty("output2")
    private List<KisPeriodChartBar> output2;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

}
