package com.dove.technicalindicator.application.dto;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndicatorCalcTrigger {

    public static final String TOPIC = "INDICATOR_CALC_TRIGGER";

    private final int eventVersion;
    private final MarketType marketType;
    private final String stockCode;
    private final LocalDate insertedDate;

    @JsonCreator
    public IndicatorCalcTrigger(
            @JsonProperty("eventVersion") int eventVersion,
            @JsonProperty("marketType") MarketType marketType,
            @JsonProperty("stockCode") String stockCode,
            @JsonProperty("insertedDate") LocalDate insertedDate) {
        this.eventVersion = eventVersion;
        this.marketType = marketType;
        this.stockCode = stockCode;
        this.insertedDate = insertedDate;
    }

    public IndicatorCalcTrigger(MarketType marketType, String stockCode) {
        this(1, marketType, stockCode, null);
    }

    public IndicatorCalcTrigger(MarketType marketType, String stockCode, LocalDate insertedDate) {
        this(1, marketType, stockCode, insertedDate);
    }

    public String messageKey() {
        return marketType.name() + "-" + stockCode;
    }
}
