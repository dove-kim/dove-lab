package com.dove.stock.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 종목 분류 값 마스터 (원문값과 화면 표시명 매핑).
 */
@Getter
@Entity
@Table(name = "STOCK_TAG_VALUE",
        uniqueConstraints = @UniqueConstraint(name = "UK_STV_FIELD_VALUE", columnNames = {"FIELD", "TAG_VALUE"}),
        indexes = @Index(name = "IDX_STV_FIELD", columnList = "FIELD"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTagValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FIELD", nullable = false, length = 20)
    @Comment("분류 차원 (TagField 이름: SECUGRP, INDUSTRY_LCLS 등)")
    private String field;

    @Column(name = "TAG_VALUE", nullable = false, length = 130)
    @Comment("KRX/KIS 분류 값 원문 (예: 주권, 반도체)")
    private String value;

    @Column(name = "LABEL", nullable = false, length = 130)
    @Comment("화면 표시명. 기본 = 원문값, 운영자가 편집 가능")
    private String label;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("최초 등록 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("최종 수정 일시")
    private LocalDateTime updatedAt;

    private StockTagValue(String field, String value) {
        this.field = field;
        this.value = value;
        this.label = value; // 표시명 기본값 = 원문
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 수집 시 신규 분류 값을 등록한다. 표시명은 원문값으로 초기화된다.
     */
    public static StockTagValue of(String field, String value) {
        return new StockTagValue(field, value);
    }

    /**
     * 표시명을 변경한다.
     */
    public void updateLabel(String label) {
        this.label = (label == null || label.isBlank()) ? this.value : label;
        this.updatedAt = LocalDateTime.now();
    }
}
