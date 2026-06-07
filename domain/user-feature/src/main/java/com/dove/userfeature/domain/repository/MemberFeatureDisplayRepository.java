package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 기능 표시 설정 저장소.
 */
public interface MemberFeatureDisplayRepository extends JpaRepository<MemberFeatureDisplay, Long> {

    Optional<MemberFeatureDisplay> findByMemberIdAndFeatureCode(Long memberId, FeatureCode featureCode);

    List<MemberFeatureDisplay> findAllByMemberId(Long memberId);
}
