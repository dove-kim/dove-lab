package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberModelGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자별 모델 점수 접근 부여 기록 저장소.
 */
public interface MemberModelGrantRepository extends JpaRepository<MemberModelGrant, Long> {

    List<MemberModelGrant> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndModelId(Long memberId, Long modelId);

    void deleteByMemberIdAndModelId(Long memberId, Long modelId);

    void deleteByModelId(Long modelId);
}
