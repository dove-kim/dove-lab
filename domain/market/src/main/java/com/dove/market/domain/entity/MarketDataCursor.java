package com.dove.market.domain.entity;

import com.dove.market.domain.enums.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "MARKET_DATA_CURSOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketDataCursor {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Comment("시장 타입 (KOSPI, KOSDAQ, KONEX)")
    private MarketType marketType;

    @Column(name = "LAST_PROCESSED_DATE", nullable = false)
    @Comment("종목·주가 처리가 완료된 마지막 날짜. 개장일·휴장일 모두 기록.")
    private LocalDate lastProcessedDate;

    public MarketDataCursor(MarketType marketType, LocalDate lastProcessedDate) {
        this.marketType = marketType;
        this.lastProcessedDate = lastProcessedDate;
    }

    public void updateLastProcessedDate(LocalDate date) {
        this.lastProcessedDate = date;
    }
}
