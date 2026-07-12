package com.dove.userfeature.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 사용자별 커스텀 지표 접근 부여 기록.
 *
 * <p>행 존재 = 해당 지표 값 접근 허용, 회수 = 행 삭제. capability는 static enum이라 런타임 지표 id를 못 담아 별도 테이블로 둔다.
 */
@Entity
@Table(
    name = "MEMBER_CUSTOM_INDICATOR_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_CUSTOM_INDICATOR_GRANT",
        columnNames = {"MEMBER_ID", "METRIC_ID"}
    ),
    indexes = {
        @Index(name = "IDX_MCIG_MEMBER_ID", columnList = "MEMBER_ID"),
        @Index(name = "IDX_MCIG_METRIC_ID", columnList = "METRIC_ID")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCustomIndicatorGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("지표 접근 부여 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Column(name = "METRIC_ID", nullable = false)
    @Comment("커스텀 지표 ID")
    private Long metricId;

    @Column(name = "GRANTED_AT", nullable = false)
    @Comment("부여 일시")
    private LocalDateTime grantedAt;

    @Column(name = "GRANTED_BY")
    @Comment("부여한 관리자 회원 ID (null = 시스템 자동)")
    private Long grantedBy;

    public static MemberCustomIndicatorGrant create(Long memberId, Long metricId, Long grantedBy) {
        MemberCustomIndicatorGrant g = new MemberCustomIndicatorGrant();
        g.memberId = memberId;
        g.metricId = metricId;
        g.grantedAt = LocalDateTime.now();
        g.grantedBy = grantedBy;
        return g;
    }
}
