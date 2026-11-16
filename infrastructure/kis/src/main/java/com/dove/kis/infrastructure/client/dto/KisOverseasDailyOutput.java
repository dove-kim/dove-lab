package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 해외주식 기간별시세 응답의 일자별 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisOverseasDailyOutput {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @JsonProperty("xymd")
    private String date;
    @JsonProperty("clos")
    private String close;

    /**
     * 기준일을 LocalDate로 변환한다. 값이 없거나 형식이 다르면 null.
     */
    public LocalDate getDateValue() {
        if (date == null || date.trim().length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim(), YMD);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 종가를 BigDecimal로 변환한다. 값이 없거나 0 이하면 null.
     */
    public BigDecimal getCloseValue() {
        if (close == null || close.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(close.trim());
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
