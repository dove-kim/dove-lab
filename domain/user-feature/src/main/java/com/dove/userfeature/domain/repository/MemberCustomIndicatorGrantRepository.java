package com.dove.userfeature.domain.repository;

import com.dove.userfeature.domain.entity.MemberCustomIndicatorGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자별 커스텀 지표 접근 부여 기록 저장소.
 */
public interface MemberCustomIndicatorGrantRepository extends JpaRepository<MemberCustomIndicatorGrant, Long> {

    List<MemberCustomIndicatorGrant> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndMetricId(Long memberId, Long metricId);

    void deleteByMemberIdAndMetricId(Long memberId, Long metricId);

    void deleteByMetricId(Long metricId);
}
