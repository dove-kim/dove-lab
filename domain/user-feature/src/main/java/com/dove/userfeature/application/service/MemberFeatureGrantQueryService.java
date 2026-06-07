package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberFeatureGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.repository.MemberFeatureGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 기능 부여 상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberFeatureGrantQueryService {

    private final MemberFeatureGrantRepository grantRepository;

    /**
     * 사용자에게 활성 부여된 기능 코드 집합을 반환한다.
     */
    public Set<FeatureCode> findActiveFeatureCodes(Long memberId) {
        return grantRepository.findAllByMemberIdAndActiveTrue(memberId).stream()
                .map(MemberFeatureGrant::getFeatureCode)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 해당 기능을 활성 부여받았는지 여부를 반환한다.
     */
    public boolean hasActiveGrant(Long memberId, FeatureCode featureCode) {
        return grantRepository.findByMemberIdAndFeatureCode(memberId, featureCode)
                .map(g -> g.isActive())
                .orElse(false);
    }
}
