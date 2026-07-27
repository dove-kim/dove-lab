package com.dove.portfolio.domain.entity;

import com.dove.portfolio.domain.converter.RebalancePlanConfigConverter;
import com.dove.portfolio.domain.value.RebalancePlanConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * 저장된 리밸런싱 계획 — 이름 + 계획 설정(종목 배분·전략 현금·슬롯 수·참여율).
 */
@Getter
@Entity
@Table(
        name = "PORTFOLIO_REBALANCE_PLAN",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PORTFOLIO_REBALANCE_PLAN_OWNER_NAME",
                columnNames = {"OWNER_MEMBER_ID", "NAME"}),
        indexes = @Index(name = "IDX_PORTFOLIO_REBALANCE_PLAN_OWNER", columnList = "OWNER_MEMBER_ID")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioRebalancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Column(name = "OWNER_MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long ownerMemberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("계획명")
    private String name;

    @Convert(converter = RebalancePlanConfigConverter.class)
    @Column(name = "ENTRIES", nullable = false, columnDefinition = "JSON")
    @Comment("계획 설정 JSON(슬롯 수·참여율·종목 배분·전략 현금)")
    private RebalancePlanConfig config;

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
     * 계획을 생성한다.
     */
    public static PortfolioRebalancePlan create(Long ownerMemberId, String name, RebalancePlanConfig config,
                                                String createdBy) {
        PortfolioRebalancePlan p = new PortfolioRebalancePlan();
        p.ownerMemberId = ownerMemberId;
        p.name = name;
        p.config = config;
        p.createdBy = createdBy;
        p.updatedBy = null;
        LocalDateTime now = LocalDateTime.now();
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    /**
     * 계획 설정을 교체한다.
     */
    public void updateConfig(RebalancePlanConfig config, String updatedBy) {
        this.config = config;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
