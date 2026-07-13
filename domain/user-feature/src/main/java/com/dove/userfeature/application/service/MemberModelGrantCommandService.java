package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberModelGrant;
import com.dove.userfeature.domain.repository.MemberModelGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자별 모델 점수 접근 부여·회수 서비스.
 *
 * <p>모델별 grant는 JWT에 싣지 않고 요청 시 조회로 검사하므로 강제 로그아웃이 필요 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberModelGrantCommandService {

    private final MemberModelGrantRepository grantRepository;

    /**
     * 사용자에게 모델 접근을 부여한다. 이미 보유 시 멱등.
     */
    public void grant(Long memberId, Long modelId, Long grantedBy) {
        if (!grantRepository.existsByMemberIdAndModelId(memberId, modelId)) {
            grantRepository.save(MemberModelGrant.create(memberId, modelId, grantedBy));
        }
    }

    /**
     * 사용자의 모델 접근을 회수한다(행 삭제).
     */
    public void revoke(Long memberId, Long modelId) {
        grantRepository.deleteByMemberIdAndModelId(memberId, modelId);
    }

    /**
     * 모델이 삭제될 때 그 모델의 모든 부여 기록을 정리한다.
     */
    public void revokeAllForModel(Long modelId) {
        grantRepository.deleteByModelId(modelId);
    }
}
