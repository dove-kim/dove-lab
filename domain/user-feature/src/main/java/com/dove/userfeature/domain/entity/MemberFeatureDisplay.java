package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 사용자별 기능 표시 설정 (순서·숨김).
 */
@Entity
@Table(
    name = "MEMBER_FEATURE_DISPLAY",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_FEATURE_DISPLAY",
        columnNames = {"MEMBER_ID", "FEATURE_CODE"}
    ),
    indexes = @Index(name = "IDX_MEMBER_FEATURE_DISPLAY_MEMBER_ID", columnList = "MEMBER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFeatureDisplay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("표시 설정 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FEATURE_CODE", nullable = false, length = 50)
    @Comment("기능 코드")
    private FeatureCode featureCode;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("모듈 내 노출 순서 (낮을수록 위, 부여 순서 기본값)")
    private int displayOrder;

    @Column(name = "HIDDEN", nullable = false)
    @Comment("숨김 여부 (권한이 있어도 메뉴에서 숨길 수 있음)")
    private boolean hidden;

    public static MemberFeatureDisplay create(Long memberId, FeatureCode featureCode, int displayOrder) {
        MemberFeatureDisplay d = new MemberFeatureDisplay();
        d.memberId = memberId;
        d.featureCode = featureCode;
        d.displayOrder = displayOrder;
        d.hidden = false;
        return d;
    }

    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
