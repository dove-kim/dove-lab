package com.dove.indicator.domain.breadth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 한 (거래소, 가격유형, 거래일)의 당일 상승비율(advance ratio)을 담는 단일 스칼라 행.
 */
@Getter
@Entity
@Table(name = "STOCK_BREADTH_DAILY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockBreadthDaily {

    @EmbeddedId
    private StockBreadthDailyId id;

    @Column(name = "ADVANCE_RATIO")
    @Comment("당일 RET_1D>0 종목 비율(0~1)")
    private Double advanceRatio;

    @Column(name = "CALCULATED_AT", nullable = false)
    @Comment("계산 일시")
    private LocalDateTime calculatedAt;

    public StockBreadthDaily(StockBreadthDailyId id, Double advanceRatio, LocalDateTime calculatedAt) {
        this.id = id;
        this.advanceRatio = advanceRatio;
        this.calculatedAt = calculatedAt;
    }
}
