package com.dove.technicalindicator.domain.entity;

import com.dove.stockdata.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기술적 지표 엔티티. 종목별 거래일의 지표 유형과 계산 값을 저장한다.
 */
@Getter
@Entity
@Table(name = "TECHNICAL_INDICATOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechnicalIndicator {

    @EmbeddedId
    private TechnicalIndicatorId id;

    @Column(name = "INDICATOR_VALUE", nullable = false)
    private Double indicatorValue;

    @Column(name = "CALCULATED_AT", nullable = false)
    private LocalDateTime calculatedAt;

    public TechnicalIndicator(MarketType marketType, String stockCode,
                              LocalDate tradeDate, IndicatorType indicatorName,
                              Double indicatorValue) {
        this.id = new TechnicalIndicatorId(marketType, stockCode, tradeDate, indicatorName);
        this.indicatorValue = indicatorValue;
        this.calculatedAt = LocalDateTime.now();
    }
}
