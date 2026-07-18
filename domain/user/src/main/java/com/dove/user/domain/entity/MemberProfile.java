package com.dove.user.domain.entity;

import com.dove.auth.domain.enums.MemberRole;
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
 * 회원 신원/프로필 정보. 비밀번호 해시는 보유하지 않는다(자격증명은 domain/auth 책임).
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

    @Column(name = "DELETED_AT")
    @Comment("탈퇴(soft delete) 일시 (null이면 활성)")
    private LocalDateTime deletedAt;

    /**
     * 신규 회원 프로필을 생성한다.
     *
     * @param email 이메일
     * @param name  표시명
     * @param role  초기 권한
     * @return 영속화 전 프로필 인스턴스
     */
    public static MemberProfile create(String email, String name, MemberRole role) {
        MemberProfile p = new MemberProfile();
        p.email = email;
        p.name = name;
        p.role = role;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    /**
     * 역할을 변경한다.
     *
     * @param role 새 역할
     */
    public void changeRole(MemberRole role) {
        this.role = role;
    }

    /**
     * 회원을 탈퇴 처리한다(soft delete). 행과 참조는 유지되고 활성 상태만 해제된다.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
