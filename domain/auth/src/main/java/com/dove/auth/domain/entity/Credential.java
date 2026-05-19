package com.dove.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 회원 자격증명. 로그인 식별자와 비밀번호 해시를 보유한다.
 *
 * <p>DOVE_AUTH 스키마의 CREDENTIAL 테이블에 매핑된다. PK는 MemberProfile.id를 논리 참조하며,
 * cross-schema FK는 두지 않는다(스키마 격리 원칙). 비밀번호 해시는 이 모듈 밖으로 절대
 * 노출되지 않으며, ArchUnit 가드레일로 강제한다.
 */
@Entity
@Table(
    name = "CREDENTIAL",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_CREDENTIAL_USERNAME", columnNames = {"USERNAME"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credential {

    @Id
    @Column(name = "MEMBER_ID")
    @Comment("회원 ID")
    private Long memberId;

    @Column(name = "USERNAME", nullable = false, length = 50)
    @Comment("로그인 식별자")
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    @Comment("BCrypt 해시")
    private String passwordHash;

    @Column(name = "LAST_LOGIN_AT")
    @Comment("마지막 성공 로그인 시각")
    private LocalDateTime lastLoginAt;

    @Column(name = "FAILED_ATTEMPTS", nullable = false)
    @Comment("연속 로그인 실패 횟수 (성공 시 0으로 초기화)")
    private int failedAttempts;

    @Column(name = "LOCKED_UNTIL")
    @Comment("계정 잠금 해제 시각 (null이면 잠금 안 됨)")
    private LocalDateTime lockedUntil;

    @Column(name = "PASSWORD_RESET_REQUIRED", nullable = false)
    @Comment("비밀번호 초기화 필요 여부 (true=다음 로그인 시 변경 강제)")
    private boolean passwordResetRequired;

    public static Credential create(Long memberId, String username, String passwordHash) {
        Credential c = new Credential();
        c.memberId = memberId;
        c.username = username;
        c.passwordHash = passwordHash;
        c.failedAttempts = 0;
        return c;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordFailedLogin() {
        this.failedAttempts += 1;
    }

    /**
     * 외부에서 결정한 잠금 해제 시점까지 계정을 잠근다.
     * 잠금 정책(임계치, 지속 시간)은 application 레이어 책임.
     */
    public void lockUntil(LocalDateTime until) {
        this.lockedUntil = until;
    }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void updatePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /** 관리자에 의한 비밀번호 초기화. 다음 로그인 시 변경 강제. */
    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordResetRequired = true;
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    /** 사용자가 직접 비밀번호 변경. 초기화 플래그 해제. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordResetRequired = false;
    }
}
