package com.dove.custommetric.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * CustomMetricDaily 복합키 — (지표 ID, 거래일). SERIES 지표는 시장 단일 스칼라라 거래일당 1행.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomMetricDailyId implements Serializable {

    @Column(name = "METRIC_ID", nullable = false)
    private Long metricId;

    @Column(name = "TRADE_DATE", nullable = false)
    private LocalDate tradeDate;
}
