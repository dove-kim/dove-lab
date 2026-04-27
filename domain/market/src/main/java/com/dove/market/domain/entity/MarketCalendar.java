package com.dove.market.domain.entity;

import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 시장 캘린더 엔티티. 날짜별 시장의 거래일/휴장일 여부를 기록한다.
 */
@Getter
@Entity
@Table(name = "MARKET_CALENDAR",
        indexes = @Index(name = "IDX_MARKET_CALENDAR_MARKET_DAYTYPE_DATE",
                columnList = "MARKET_TYPE, DAY_TYPE, DATE"))
@IdClass(MarketCalendarId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarketCalendar {
    @Id
    @Column(name = "DATE", nullable = false)
    private LocalDate date;

    @Id
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MarketType marketType;

    @Column(name = "DAY_TYPE", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MarketDayType dayType;

    public void updateDayType(MarketDayType dayType) {
        this.dayType = dayType;
    }
}
