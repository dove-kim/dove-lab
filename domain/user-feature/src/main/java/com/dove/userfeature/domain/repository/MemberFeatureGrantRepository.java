package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberFeatureGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 기능 부여 기록 저장소.
 */
public interface MemberFeatureGrantRepository extends JpaRepository<MemberFeatureGrant, Long> {

    Optional<MemberFeatureGrant> findByMemberIdAndFeatureCode(Long memberId, FeatureCode featureCode);

    List<MemberFeatureGrant> findAllByMemberIdAndActiveTrue(Long memberId);

    List<MemberFeatureGrant> findAllByMemberId(Long memberId);
}
