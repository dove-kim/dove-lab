package com.dove.userfeature.domain.entity;

import com.dove.userfeature.domain.capability.Capability;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 사용자 capability 부여 기록.
 *
 * <p>행 존재 = 권한 보유, 회수 = 행 삭제(soft 플래그 없음).
 */
@Entity
@Table(
    name = "MEMBER_CAPABILITY_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_CAPABILITY_GRANT",
        columnNames = {"MEMBER_ID", "CAPABILITY"}
    ),
    indexes = @Index(name = "IDX_MEMBER_CAPABILITY_GRANT_MEMBER_ID", columnList = "MEMBER_ID")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCapabilityGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("권한 부여 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CAPABILITY", nullable = false, length = 50)
    @Comment("권한 코드")
    private Capability capability;

    @Column(name = "GRANTED_AT", nullable = false)
    @Comment("부여 일시")
    private LocalDateTime grantedAt;

    @Column(name = "GRANTED_BY")
    @Comment("부여한 관리자 회원 ID (null = 시스템 자동)")
    private Long grantedBy;

    public static MemberCapabilityGrant create(Long memberId, Capability capability, Long grantedBy) {
        MemberCapabilityGrant g = new MemberCapabilityGrant();
        g.memberId = memberId;
        g.capability = capability;
        g.grantedAt = LocalDateTime.now();
        g.grantedBy = grantedBy;
        return g;
    }
}
