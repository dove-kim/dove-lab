package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 사용자별 기능 표시 설정 (순서·숨김).
 *
 * <p>기능 부여(grant) 시 자동 생성되며, 기능 회수 후에도 유지된다.
 * 재부여 시 이전 순서가 그대로 복원된다.
 */
@Entity
@Table(
    name = "USER_FEATURE_DISPLAY",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_USER_FEATURE_DISPLAY",
        columnNames = {"USER_ID", "FEATURE_CODE"}
    ),
    indexes = @Index(name = "IDX_USER_FEATURE_DISPLAY_USER_ID", columnList = "USER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeatureDisplay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("표시 설정 고유 ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    @Comment("회원 ID")
    private Long userId;

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

    public static UserFeatureDisplay create(Long userId, FeatureCode featureCode, int displayOrder) {
        UserFeatureDisplay d = new UserFeatureDisplay();
        d.userId = userId;
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
