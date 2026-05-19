package com.dove.screening.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "INDICATOR_PRESET",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_INDICATOR_PRESET_MEMBER_NAME", columnNames = {"MEMBER_ID", "NAME"})
    },
    indexes = {
        @Index(name = "IDX_INDICATOR_PRESET_MEMBER_ID", columnList = "MEMBER_ID")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndicatorPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("프리셋 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long memberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("프리셋 이름 (사용자 내 고유)")
    private String name;

    @Column(name = "ITEMS", nullable = false, columnDefinition = "TEXT")
    @Comment("지표 설정 목록 (JSON 배열: [{type, enabled, color, lineWidth}])")
    private String items;

    @Column(name = "PANEL_ORDER", length = 300)
    @Comment("서브패널 노출 순서 (쉼표 구분 PanelId)")
    private String panelOrder;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("목록 노출 순서 (낮을수록 위)")
    private int displayOrder;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public static IndicatorPreset create(Long memberId, String name, String items, String panelOrder) {
        IndicatorPreset p = new IndicatorPreset();
        p.memberId   = memberId;
        p.name       = name;
        p.items      = items;
        p.panelOrder = panelOrder;
        p.createdAt  = LocalDateTime.now();
        p.updatedAt  = LocalDateTime.now();
        return p;
    }

    public void update(String name, String items, String panelOrder) {
        this.name       = name;
        this.items      = items;
        this.panelOrder = panelOrder;
        this.updatedAt  = LocalDateTime.now();
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }
}
