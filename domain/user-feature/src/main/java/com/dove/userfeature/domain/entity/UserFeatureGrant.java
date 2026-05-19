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
 *
 * <p>active=true이면 해당 기능을 사용할 수 있다.
 * 기능 회수(revoke) 시 active=false로 설정하며 행은 유지한다.
 * 재부여 시 active=true로 복구한다.
 */
@Entity
@Table(
    name = "USER_FEATURE_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_USER_FEATURE_GRANT",
        columnNames = {"USER_ID", "FEATURE_CODE"}
    ),
    indexes = @Index(name = "IDX_USER_FEATURE_GRANT_USER_ID", columnList = "USER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeatureGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("기능 부여 고유 ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    @Comment("회원 ID")
    private Long userId;

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

    public static UserFeatureGrant create(Long userId, FeatureCode featureCode, Long grantedBy) {
        UserFeatureGrant g = new UserFeatureGrant();
        g.userId = userId;
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
