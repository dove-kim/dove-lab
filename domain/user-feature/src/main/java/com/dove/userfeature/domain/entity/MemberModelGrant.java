package com.dove.userfeature.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 사용자별 모델 점수 접근 부여 기록.
 *
 * <p>행 존재 = 해당 모델 점수 접근 허용, 회수 = 행 삭제. 모델 id는 런타임 등록값이라 capability enum에 담지 못해 별도 테이블로 둔다.
 */
@Entity
@Table(
    name = "MEMBER_MODEL_GRANT",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_MODEL_GRANT",
        columnNames = {"MEMBER_ID", "MODEL_ID"}
    ),
    indexes = {
        @Index(name = "IDX_MMG_MEMBER_ID", columnList = "MEMBER_ID"),
        @Index(name = "IDX_MMG_MODEL_ID", columnList = "MODEL_ID")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberModelGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("모델 접근 부여 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("회원 ID")
    private Long memberId;

    @Column(name = "MODEL_ID", nullable = false)
    @Comment("모델 ID")
    private Long modelId;

    @Column(name = "GRANTED_AT", nullable = false)
    @Comment("부여 일시")
    private LocalDateTime grantedAt;

    @Column(name = "GRANTED_BY")
    @Comment("부여한 관리자 회원 ID (null = 시스템 자동)")
    private Long grantedBy;

    public static MemberModelGrant create(Long memberId, Long modelId, Long grantedBy) {
        MemberModelGrant g = new MemberModelGrant();
        g.memberId = memberId;
        g.modelId = modelId;
        g.grantedAt = LocalDateTime.now();
        g.grantedBy = grantedBy;
        return g;
    }
}
