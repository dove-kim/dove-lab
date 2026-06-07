package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.enums.FeatureCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 사용자 기능 부여 기록.
 */
@Entity
@Table(
    name = "MEMBER_FEATURE_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_FEATURE_GRANT",
        columnNames = {"MEMBER_ID", "FEATURE_CODE"}
    ),
    indexes = @Index(name = "IDX_MEMBER_FEATURE_GRANT_MEMBER_ID", columnList = "MEMBER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFeatureGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("기능 부여 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FEATURE_CODE", nullable = false, length = 50)
    @Comment("기능 코드")
    private FeatureCode featureCode;

    @Column(name = "ACTIVE", nullable = false)
    @Comment("활성 여부 (false = 회수됨)")
    private boolean active;

    @Column(name = "GRANTED_AT", nullable = false)
    @Comment("최초 부여 일시")
    private LocalDateTime grantedAt;

    @Column(name = "GRANTED_BY")
    @Comment("부여한 관리자 회원 ID (null = 시스템 자동)")
    private Long grantedBy;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("마지막 상태 변경 일시")
    private LocalDateTime updatedAt;

    public static MemberFeatureGrant create(Long memberId, FeatureCode featureCode, Long grantedBy) {
        MemberFeatureGrant g = new MemberFeatureGrant();
        g.memberId = memberId;
        g.featureCode = featureCode;
        g.active = true;
        g.grantedAt = LocalDateTime.now();
        g.grantedBy = grantedBy;
        g.updatedAt = LocalDateTime.now();
        return g;
    }

    public void activate(Long grantedBy) {
        this.active = true;
        this.grantedBy = grantedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void revoke() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
