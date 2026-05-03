package com.dove.member.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "INVITE_CODE",
    uniqueConstraints = @UniqueConstraint(name = "UK_INVITE_CODE", columnNames = {"CODE"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("초대 코드 고유 ID")
    private Long id;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Comment("낙관적 락 버전")
    private Long version;

    @Column(name = "CODE", nullable = false, length = 64)
    @Comment("초대 코드 값 (UUID 기반)")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    @Comment("발급 대상 역할 (USER/ADMIN)")
    private MemberRole role;

    @Column(name = "EXPIRES_AT", nullable = false)
    @Comment("만료일시")
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    @Comment("사용일시 (NULL이면 미사용)")
    private LocalDateTime usedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("발급자 username")
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("발급일시")
    private LocalDateTime createdAt;

    public static InviteCode create(MemberRole role, LocalDateTime expiresAt, String createdBy) {
        InviteCode c = new InviteCode();
        c.code = UUID.randomUUID().toString().replace("-", "");
        c.role = role;
        c.expiresAt = expiresAt;
        c.createdBy = createdBy;
        c.createdAt = LocalDateTime.now();
        return c;
    }

    public boolean isValid() {
        return usedAt == null && LocalDateTime.now().isBefore(expiresAt);
    }

    public void use() {
        this.usedAt = LocalDateTime.now();
    }
}
