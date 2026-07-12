package com.dove.custommetric.domain.entity;

import com.dove.stock.domain.enums.PriceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 커스텀 지표 정의 — ROOT가 등록하는 계산식(DSL 스펙)과 상태. 전략을 데이터로 담는 단위.
 */
@Getter
@Entity
@Table(name = "CUSTOM_METRIC_DEF",
        uniqueConstraints = @UniqueConstraint(name = "UK_CMD_NAME", columnNames = {"NAME"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomMetricDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("지표 고유 ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("지표 이름(고유)")
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    @Comment("설명")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "SHAPE", nullable = false, length = 10)
    @Comment("출력 모양 (SERIES=시장 스칼라 / PANEL=종목별)")
    private MetricShape shape;

    @Column(name = "SPEC", nullable = false, columnDefinition = "TEXT")
    @Comment("계산식 DSL(JSON 트리)")
    private String spec;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", nullable = false, length = 10)
    @Comment("피처 읽을 주가 유형 (RAW/ADJUSTED)")
    private PriceType priceType;

    @Column(name = "ACTIVE", nullable = false)
    @Comment("활성 여부 (FALSE=야간 계산 스킵·숨김)")
    private boolean active;

    @Column(name = "LAST_COMPUTED_DATE")
    @Comment("마지막으로 계산·저장된 거래일. NULL=미계산")
    private LocalDate lastComputedDate;

    @Column(name = "LAST_ERROR", length = 500)
    @Comment("마지막 계산 오류 메시지. NULL=정상")
    private String lastError;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("생성자 username")
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /**
     * 커스텀 지표 정의를 생성한다. 초기 활성.
     */
    public static CustomMetricDef create(String name, String description, MetricShape shape,
                                         String spec, PriceType priceType, String createdBy) {
        CustomMetricDef d = new CustomMetricDef();
        d.name = name;
        d.description = description;
        d.shape = shape;
        d.spec = spec;
        d.priceType = priceType != null ? priceType : PriceType.RAW;
        d.active = true;
        d.createdBy = createdBy;
        LocalDateTime now = LocalDateTime.now();
        d.createdAt = now;
        d.updatedAt = now;
        return d;
    }

    /**
     * 이름·설명·계산식·주가유형을 갱신한다. 스펙이 바뀌면 재계산이 필요하므로 진행 상태를 초기화한다.
     */
    public void update(String name, String description, String spec, PriceType priceType) {
        boolean specChanged = !this.spec.equals(spec);
        this.name = name;
        this.description = description;
        this.spec = spec;
        this.priceType = priceType != null ? priceType : PriceType.RAW;
        if (specChanged) {
            this.lastComputedDate = null;
            this.lastError = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활성 여부를 변경한다.
     */
    public void updateActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계산 완료 지점(마지막 거래일)을 기록하고 오류를 지운다.
     */
    public void recordComputed(LocalDate lastDate) {
        this.lastComputedDate = lastDate;
        this.lastError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계산 오류를 기록한다.
     */
    public void recordError(String message) {
        this.lastError = message != null && message.length() > 500 ? message.substring(0, 500) : message;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 진행 상태를 초기화한다(재계산 트리거용) — 커서를 처음으로 되돌린다.
     */
    public void resetProgress() {
        this.lastComputedDate = null;
        this.lastError = null;
        this.updatedAt = LocalDateTime.now();
    }
}
