package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 해외주식 현재체결가 응답의 output 항목.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisOverseasPriceOutput {

    @JsonProperty("last")
    private String last;
    @JsonProperty("base")
    private String base;

    /**
     * 현재가(원통화)를 BigDecimal로 변환한다. 값이 없으면 null.
     */
    public BigDecimal getLastDecimal() {
        if (last == null || last.isBlank()) {
            return null;
        }
        return new BigDecimal(last.trim());
    }
}
