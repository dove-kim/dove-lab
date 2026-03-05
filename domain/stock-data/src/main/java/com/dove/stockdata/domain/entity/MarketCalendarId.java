package com.dove.stockdata.domain.entity;

import com.dove.stockdata.domain.enums.MarketType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MarketCalendar 엔티티의 복합키. 날짜와 시장 유형 조합.
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MarketCalendarId implements Serializable {
    private LocalDate date;
    private MarketType marketType;
}
