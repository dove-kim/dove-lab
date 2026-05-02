package com.dove.technicalindicator.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndicatorCalcRequest {

    public static final String TOPIC = "INDICATOR_CALC_REQUESTED";

    private final int eventVersion;
    private final MarketType marketType;
    private final String stockCode;
    private final IndicatorType indicatorType;
    private final LocalDate insertedDate;

    @JsonCreator
    public IndicatorCalcRequest(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("marketType") MarketType marketType,
            @JsonProperty("stockCode") String stockCode,
            @JsonProperty("indicatorType") IndicatorType indicatorType,
            @JsonProperty("insertedDate") LocalDate insertedDate) {
        this.eventVersion = eventVersion;
        this.marketType = marketType;
        this.stockCode = stockCode;
        this.indicatorType = indicatorType;
        this.insertedDate = insertedDate;
    }

    /** per-calculator 체이닝 */
    public IndicatorCalcRequest(MarketType marketType, String stockCode, IndicatorType indicatorType) {
        this(1, marketType, stockCode, indicatorType, null);
    }

    /** per-calculator 최초 — 팬아웃에서 insertedDate 전달 */
    public IndicatorCalcRequest(MarketType marketType, String stockCode, IndicatorType indicatorType, LocalDate insertedDate) {
        this(1, marketType, stockCode, indicatorType, insertedDate);
    }

    public String messageKey() {
        return marketType.name() + "-" + stockCode + "-" + indicatorType.name();
    }
}
