package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberCustomIndicatorGrant;
import com.dove.userfeature.domain.repository.MemberCustomIndicatorGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자별 커스텀 지표 접근 부여·회수 서비스.
 *
 * <p>지표별 grant는 JWT에 싣지 않고 요청 시 조회로 검사하므로 강제 로그아웃이 필요 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCustomIndicatorGrantCommandService {

    private final MemberCustomIndicatorGrantRepository grantRepository;

    /**
     * 사용자에게 지표 접근을 부여한다. 이미 보유 시 멱등.
     */
    public void grant(Long memberId, Long metricId, Long grantedBy) {
        if (!grantRepository.existsByMemberIdAndMetricId(memberId, metricId)) {
            grantRepository.save(MemberCustomIndicatorGrant.create(memberId, metricId, grantedBy));
        }
    }

    /**
     * 사용자의 지표 접근을 회수한다(행 삭제).
     */
    public void revoke(Long memberId, Long metricId) {
        grantRepository.deleteByMemberIdAndMetricId(memberId, metricId);
    }

    /**
     * 지표가 삭제될 때 그 지표의 모든 부여 기록을 정리한다.
     */
    public void revokeAllForMetric(Long metricId) {
        grantRepository.deleteByMetricId(metricId);
    }
}
