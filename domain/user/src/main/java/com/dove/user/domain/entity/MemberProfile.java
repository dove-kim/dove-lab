package com.dove.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 회원 신원/프로필 정보.
 *
 * <p>DOVE_USER 스키마의 MEMBER 테이블에 매핑된다. 비밀번호 해시는 보유하지 않으며,
 * 자격증명은 별도 도메인(domain/auth)의 Credential이 책임진다.
 */
@Entity
@Table(
    name = "MEMBER",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_MEMBER_EMAIL", columnNames = {"EMAIL"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("회원 고유 ID")
    private Long id;

    @Column(name = "EMAIL", nullable = false, length = 100)
    @Comment("이메일 (식별자)")
    private String email;

    @Column(name = "NAME", nullable = false, length = 50)
    @Comment("표시명")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    @Comment("권한 (USER/ADMIN/ROOT)")
    private MemberRole role;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("가입일시")
    private LocalDateTime createdAt;

    public static MemberProfile create(String email, String name, MemberRole role) {
        MemberProfile p = new MemberProfile();
        p.email = email;
        p.name = name;
        p.role = role;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    public boolean hasRole(MemberRole required) {
        return this.role == required;
    }

    public void changeRole(MemberRole role) {
        this.role = role;
    }
}
