package com.dove.custommetric.domain.entity;

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
 * 커스텀 지표(SERIES)의 거래일별 계산값 — 시장 단일 스칼라.
 */
@Getter
@Entity
@Table(name = "CUSTOM_METRIC_DAILY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomMetricDaily {

    @EmbeddedId
    private CustomMetricDailyId id;

    @Column(name = "VALUE", nullable = false)
    @Comment("지표 계산값")
    private Double value;

    @Column(name = "CALCULATED_AT", nullable = false)
    @Comment("계산 일시")
    private LocalDateTime calculatedAt;

    public CustomMetricDaily(CustomMetricDailyId id, double value, LocalDateTime calculatedAt) {
        this.id = id;
        this.value = value;
        this.calculatedAt = calculatedAt;
    }
}
