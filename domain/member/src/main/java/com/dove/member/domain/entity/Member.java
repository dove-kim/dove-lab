package com.dove.member.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "MEMBER",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_MEMBER_USERNAME", columnNames = {"USERNAME"}),
        @UniqueConstraint(name = "UK_MEMBER_EMAIL",    columnNames = {"EMAIL"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("회원 고유 ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, length = 50)
    @Comment("아이디 (로그인용)")
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    @Comment("비밀번호 (BCrypt 해시)")
    private String password;

    @Column(name = "EMAIL", nullable = false, length = 100)
    @Comment("이메일")
    private String email;

    @Column(name = "NAME", nullable = false, length = 50)
    @Comment("이름")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    @Comment("권한 (USER/ADMIN)")
    private MemberRole role;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("가입일시")
    private LocalDateTime createdAt;

    public static Member create(String username, String encodedPassword, String email, String name, MemberRole role) {
        Member m = new Member();
        m.username = username;
        m.password = encodedPassword;
        m.email = email;
        m.name = name;
        m.role = role;
        m.createdAt = LocalDateTime.now();
        return m;
    }
}
