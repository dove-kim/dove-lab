package com.dove.stockdata.entity;

import com.dove.stockdata.enums.MarketDayType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "MARKET_CALENDAR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarketCalendar {
    @Id
    @Column(name = "DATE", nullable = false)
    private LocalDate date;

    @Column(name = "DAY_TYPE", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MarketDayType dayType;
}
