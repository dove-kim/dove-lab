package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberCustomIndicatorGrant;
import com.dove.userfeature.domain.repository.MemberCustomIndicatorGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자별 커스텀 지표 접근 부여 상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberCustomIndicatorGrantQueryService {

    private final MemberCustomIndicatorGrantRepository grantRepository;

    /**
     * 사용자에게 부여된 지표 ID 집합을 반환한다.
     */
    public Set<Long> findGrantedMetricIds(Long memberId) {
        return grantRepository.findAllByMemberId(memberId).stream()
                .map(MemberCustomIndicatorGrant::getMetricId)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 해당 지표 접근을 보유했는지 여부를 반환한다.
     */
    public boolean hasGrant(Long memberId, Long metricId) {
        return grantRepository.existsByMemberIdAndMetricId(memberId, metricId);
    }
}
