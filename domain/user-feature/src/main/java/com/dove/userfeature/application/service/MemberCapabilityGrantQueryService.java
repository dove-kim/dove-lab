package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.capability.Capability;
import com.dove.userfeature.domain.entity.MemberCapabilityGrant;
import com.dove.userfeature.domain.repository.MemberCapabilityGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 capability 부여 상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCapabilityGrantQueryService {

    private final MemberCapabilityGrantRepository grantRepository;

    /**
     * 사용자에게 부여된 capability 집합을 반환한다.
     */
    public Set<Capability> findGrantedCapabilities(Long memberId) {
        return grantRepository.findAllByMemberId(memberId).stream()
                .map(MemberCapabilityGrant::getCapability)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 해당 capability를 보유했는지 여부를 반환한다.
     */
    public boolean hasGrant(Long memberId, Capability capability) {
        return grantRepository.existsByMemberIdAndCapability(memberId, capability);
    }
}
