package com.dove.technicalindicator.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndicatorCursorId {

    @Column(name = "MARKET_TYPE", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Comment("시장 구분 (KOSPI/KOSDAQ/KONEX)")
    private MarketType marketType;

    @Column(name = "CODE", nullable = false, length = 20)
    @Comment("종목 단축코드")
    private String code;

    @Column(name = "INDICATOR_TYPE", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Comment("calculator 대표 IndicatorType (cursor 식별자)")
    private IndicatorType indicatorType;
}
