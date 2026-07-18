package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.enums.PortfolioSharePermission;
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
 * 계좌 단위 공유 grant — 소유자가 다른 회원에게 특정 계좌의 열람/쓰기 권한을 부여한 레코드.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_SHARE",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PORTFOLIO_SHARE_ACCOUNT_GRANTEE", columnNames = {"ACCOUNT_ID", "GRANTEE_MEMBER_ID"}),
        indexes = {
                @Index(name = "IDX_PORTFOLIO_SHARE_OWNER", columnList = "OWNER_MEMBER_ID"),
                @Index(name = "IDX_PORTFOLIO_SHARE_GRANTEE", columnList = "GRANTEE_MEMBER_ID")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Column(name = "OWNER_MEMBER_ID", nullable = false)
    @Comment("공유한 소유 회원 ID")
    private Long ownerMemberId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    @Comment("공유 대상 계좌 ID(PORTFOLIO_ACCOUNT.ID)")
    private Long accountId;

    @Column(name = "GRANTEE_MEMBER_ID", nullable = false)
    @Comment("공유받은 회원 ID")
    private Long granteeMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERMISSION", nullable = false, length = 20)
    @Comment("공유 권한(READ/READ_RELATIVE/WRITE)")
    private PortfolioSharePermission permission;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("생성자 username")
    private String createdBy;

    /**
     * 공유 grant를 생성한다.
     */
    public static PortfolioShare create(Long ownerMemberId, Long accountId, Long granteeMemberId,
                                        PortfolioSharePermission permission, String createdBy) {
        PortfolioShare s = new PortfolioShare();
        s.ownerMemberId = ownerMemberId;
        s.accountId = accountId;
        s.granteeMemberId = granteeMemberId;
        s.permission = permission;
        s.createdBy = createdBy;
        s.createdAt = LocalDateTime.now();
        return s;
    }

    /**
     * 공유 권한을 변경한다.
     */
    public void changePermission(PortfolioSharePermission permission) {
        this.permission = permission;
    }
}
