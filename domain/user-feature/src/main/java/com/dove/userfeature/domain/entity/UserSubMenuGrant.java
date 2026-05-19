package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.enums.SubMenuCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 사용자 하위 메뉴 부여 기록.
 *
 * <p>기능 부여(grant) 시 소속 하위 메뉴가 자동으로 함께 부여된다.
 * ROOT가 개별 하위 메뉴를 추가 회수하거나 재부여할 수 있다.
 */
@Entity
@Table(
    name = "USER_SUB_MENU_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_USER_SUB_MENU_GRANT",
        columnNames = {"USER_ID", "SUB_MENU_CODE"}
    ),
    indexes = @Index(name = "IDX_USER_SUB_MENU_GRANT_USER_ID", columnList = "USER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubMenuGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("하위 메뉴 부여 고유 ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    @Comment("회원 ID")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SUB_MENU_CODE", nullable = false, length = 50)
    @Comment("하위 메뉴 코드")
    private SubMenuCode subMenuCode;

    @Column(name = "ACTIVE", nullable = false)
    @Comment("활성 여부 (false = 회수됨)")
    private boolean active;

    @Column(name = "GRANTED_AT", nullable = false)
    @Comment("최초 부여 일시")
    private LocalDateTime grantedAt;

    @Column(name = "GRANTED_BY")
    @Comment("부여한 관리자 회원 ID (null = 기능 부여 시 자동)")
    private Long grantedBy;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("마지막 상태 변경 일시")
    private LocalDateTime updatedAt;

    public static UserSubMenuGrant create(Long userId, SubMenuCode subMenuCode, Long grantedBy) {
        UserSubMenuGrant g = new UserSubMenuGrant();
        g.userId = userId;
        g.subMenuCode = subMenuCode;
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
