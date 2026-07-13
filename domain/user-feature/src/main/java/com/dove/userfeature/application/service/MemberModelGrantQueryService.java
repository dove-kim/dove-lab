package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberModelGrant;
import com.dove.userfeature.domain.repository.MemberModelGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자별 모델 점수 접근 부여 상태 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberModelGrantQueryService {

    private final MemberModelGrantRepository grantRepository;

    /**
     * 사용자에게 부여된 모델 ID 집합을 반환한다.
     */
    public Set<Long> findGrantedModelIds(Long memberId) {
        return grantRepository.findAllByMemberId(memberId).stream()
                .map(MemberModelGrant::getModelId)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 해당 모델 접근을 보유했는지 여부를 반환한다.
     */
    public boolean hasGrant(Long memberId, Long modelId) {
        return grantRepository.existsByMemberIdAndModelId(memberId, modelId);
    }
}
