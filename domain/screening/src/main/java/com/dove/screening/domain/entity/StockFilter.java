package com.dove.screening.domain.entity;

import com.dove.screening.domain.converter.NumericConditionListConverter;
import com.dove.screening.domain.converter.StockConditionListConverter;
import com.dove.screening.domain.converter.TagConditionListConverter;
import com.dove.screening.domain.value.NumericCondition;
import com.dove.screening.domain.value.StockCondition;
import com.dove.screening.domain.value.TagCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;

/**
 * 종목 필터 (시스템·개인 통합). memberId 가 null 이면 시스템 필터, 있으면 개인 필터.
 */
@Getter
@Entity
@Table(
        name = "STOCK_FILTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_STOCK_FILTER_MEMBER_NAME", columnNames = {"MEMBER_ID", "NAME"})
        },
        indexes = {
                @Index(name = "IDX_STOCK_FILTER_OWNER", columnList = "MEMBER_ID,ENABLED,DISPLAY_ORDER")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID")
    @Comment("NULL=시스템 필터, NOT NULL=개인 필터")
    private Long memberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("필터명")
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    @Comment("설명")
    private String description;

    @Convert(converter = TagConditionListConverter.class)
    @Column(name = "TAG_CONDITIONS", nullable = false, columnDefinition = "JSON")
    @Comment("태그 조건 JSON 배열")
    private List<TagCondition> tagConditions;

    @Convert(converter = StockConditionListConverter.class)
    @Column(name = "STOCK_CONDITIONS", nullable = false, columnDefinition = "JSON")
    @Comment("종목 조건 JSON 배열")
    private List<StockCondition> stockConditions;

    @Convert(converter = NumericConditionListConverter.class)
    @Column(name = "NUMERIC_CONDITIONS", nullable = false, columnDefinition = "JSON")
    @Comment("수치 범위 조건 JSON 배열 (자본금·액면가·상장주식수 등)")
    private List<NumericCondition> numericConditions;

    @Column(name = "ENABLED", nullable = false)
    @Comment("활성 여부 (FALSE=picker 숨김)")
    private boolean enabled;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("표시 순서 (낮을수록 위)")
    private int displayOrder;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("생성자 username")
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    @Comment("마지막 수정자 username")
    private String updatedBy;

    // ── 정적 팩토리 ─────────────────────────────────────────────────────────

    /**
     * 시스템 종목 필터를 생성한다.
     */
    public static StockFilter createSystem(String name, String description,
                                           List<TagCondition> tagConditions,
                                           List<StockCondition> stockConditions,
                                           List<NumericCondition> numericConditions,
                                           String createdBy) {
        StockFilter f = new StockFilter();
        f.memberId = null;
        f.name = name;
        f.description = description;
        f.tagConditions = tagConditions != null ? tagConditions : List.of();
        f.stockConditions = stockConditions != null ? stockConditions : List.of();
        f.numericConditions = numericConditions != null ? numericConditions : List.of();
        f.enabled = true;
        f.displayOrder = 0;
        f.createdBy = createdBy;
        f.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        f.createdAt = now;
        f.updatedAt = now;
        return f;
    }

    /**
     * 개인 종목 필터를 생성한다.
     *
     * @throws IllegalArgumentException memberId가 null일 때
     */
    public static StockFilter createPersonal(Long memberId, String name, String description,
                                             List<TagCondition> tagConditions,
                                             List<StockCondition> stockConditions,
                                             List<NumericCondition> numericConditions,
                                             String createdBy) {
        if (memberId == null) {
            throw new IllegalArgumentException("MEMBER_ID_REQUIRED");
        }
        StockFilter f = new StockFilter();
        f.memberId = memberId;
        f.name = name;
        f.description = description;
        f.tagConditions = tagConditions != null ? tagConditions : List.of();
        f.stockConditions = stockConditions != null ? stockConditions : List.of();
        f.numericConditions = numericConditions != null ? numericConditions : List.of();
        f.enabled = true;
        f.displayOrder = 0;
        f.createdBy = createdBy;
        f.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        f.createdAt = now;
        f.updatedAt = now;
        return f;
    }

    // ── 상태 조회 ───────────────────────────────────────────────────────────

    public boolean isSystem() {
        return memberId == null;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId != null && this.memberId.equals(memberId);
    }

    // ── 상태 변경 ───────────────────────────────────────────────────────────

    /**
     * 필터명·설명·조건 목록을 갱신한다.
     */
    public void update(String name, String description,
                       List<TagCondition> tagConditions,
                       List<StockCondition> stockConditions,
                       List<NumericCondition> numericConditions,
                       String updatedBy) {
        this.name = name;
        this.description = description;
        this.tagConditions = tagConditions != null ? tagConditions : List.of();
        this.stockConditions = stockConditions != null ? stockConditions : List.of();
        this.numericConditions = numericConditions != null ? numericConditions : List.of();
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활성 여부를 변경한다.
     */
    public void updateEnabled(boolean enabled, String updatedBy) {
        this.enabled = enabled;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }
}
