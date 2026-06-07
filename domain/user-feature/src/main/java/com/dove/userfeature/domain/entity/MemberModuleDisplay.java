package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.enums.ModuleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 사용자별 모듈 표시 설정 (순서·숨김).
 */
@Entity
@Table(
    name = "MEMBER_MODULE_DISPLAY",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_MODULE_DISPLAY",
        columnNames = {"MEMBER_ID", "MODULE_CODE"}
    ),
    indexes = @Index(name = "IDX_MEMBER_MODULE_DISPLAY_MEMBER_ID", columnList = "MEMBER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberModuleDisplay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("모듈 표시 설정 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODULE_CODE", nullable = false, length = 50)
    @Comment("모듈 코드")
    private ModuleCode moduleCode;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    @Comment("모듈 노출 순서 (낮을수록 위, 첫 기능 부여 순서 기본값)")
    private int displayOrder;

    @Column(name = "HIDDEN", nullable = false)
    @Comment("숨김 여부 (모듈 전체를 메뉴에서 숨길 수 있음)")
    private boolean hidden;

    public static MemberModuleDisplay create(Long memberId, ModuleCode moduleCode, int displayOrder) {
        MemberModuleDisplay d = new MemberModuleDisplay();
        d.memberId = memberId;
        d.moduleCode = moduleCode;
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
