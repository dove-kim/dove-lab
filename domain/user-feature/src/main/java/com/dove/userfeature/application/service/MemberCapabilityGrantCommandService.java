package com.dove.userfeature.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.userfeature.domain.capability.Capability;
import com.dove.userfeature.domain.entity.MemberCapabilityGrant;
import com.dove.userfeature.domain.repository.MemberCapabilityGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 capability 부여·회수 서비스.
 *
 * <p>표시 설정(노출·순서)은 다루지 않는다 — 권한과 표시는 분리된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberCapabilityGrantCommandService {

    private final MemberCapabilityGrantRepository grantRepository;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 사용자에게 capability를 부여한다. 이미 보유 시 멱등.
     */
    public void grant(Long memberId, Capability capability, Long grantedBy) {
        if (!grantRepository.existsByMemberIdAndCapability(memberId, capability)) {
            grantRepository.save(MemberCapabilityGrant.create(memberId, capability, grantedBy));
        }
        forcedLogoutService.markLogoutNow(memberId);
    }

    /**
     * 사용자의 capability를 회수한다(행 삭제).
     */
    public void revoke(Long memberId, Capability capability) {
        grantRepository.deleteByMemberIdAndCapability(memberId, capability);
        forcedLogoutService.markLogoutNow(memberId);
    }
}
