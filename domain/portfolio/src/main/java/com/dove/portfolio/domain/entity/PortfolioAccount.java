package com.dove.portfolio.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 포트폴리오 계좌 — 현금·공유·암호화의 분리 단위. 현금·입출금은 원화 기준.
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_ACCOUNT",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PORTFOLIO_ACCOUNT_OWNER_NAME", columnNames = {"OWNER_MEMBER_ID", "NAME"})
        },
        indexes = {
                @Index(name = "IDX_PORTFOLIO_ACCOUNT_OWNER", columnList = "OWNER_MEMBER_ID")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Column(name = "OWNER_MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long ownerMemberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("계좌명")
    private String name;

    @Column(name = "BROKER_NAME", length = 100)
    @Comment("증권사명")
    private String brokerName;

    @Column(name = "DESCRIPTION", length = 500)
    @Comment("설명")
    private String description;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Comment("생성자 username")
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 50)
    @Comment("마지막 수정자 username")
    private String updatedBy;

    /**
     * 계좌를 생성한다.
     *
     * @throws IllegalArgumentException ownerMemberId가 null일 때
     */
    public static PortfolioAccount create(Long ownerMemberId, String name, String brokerName, String description, String createdBy) {
        if (ownerMemberId == null) {
            throw new IllegalArgumentException("OWNER_MEMBER_ID_REQUIRED");
        }
        PortfolioAccount a = new PortfolioAccount();
        a.ownerMemberId = ownerMemberId;
        a.name = name;
        a.brokerName = brokerName;
        a.description = description;
        a.createdBy = createdBy;
        a.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        a.createdAt = now;
        a.updatedAt = now;
        return a;
    }

    /**
     * 계좌명·증권사·설명을 갱신한다.
     */
    public void update(String name, String brokerName, String description, String updatedBy) {
        this.name = name;
        this.brokerName = brokerName;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.ownerMemberId != null && this.ownerMemberId.equals(memberId);
    }
}
