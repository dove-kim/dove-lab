package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.capability.Capability;
import com.dove.userfeature.domain.entity.MemberCapabilityGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자 capability 부여 기록 저장소.
 */
public interface MemberCapabilityGrantRepository extends JpaRepository<MemberCapabilityGrant, Long> {

    List<MemberCapabilityGrant> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndCapability(Long memberId, Capability capability);

    void deleteByMemberIdAndCapability(Long memberId, Capability capability);
}
