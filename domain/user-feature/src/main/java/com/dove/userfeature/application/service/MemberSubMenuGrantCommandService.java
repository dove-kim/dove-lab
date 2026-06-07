package com.dove.userfeature.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.userfeature.domain.entity.MemberSubMenuGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.MemberSubMenuGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 하위 메뉴 부여·회수 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberSubMenuGrantCommandService {

    private final MemberSubMenuGrantRepository repository;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 단일 하위 메뉴를 부여한다.
     */
    public void grant(Long memberId, SubMenuCode subMenuCode, Long grantedBy) {
        grantInternal(memberId, subMenuCode, grantedBy);
        forcedLogoutService.markLogoutNow(memberId);
    }

    /**
     * 단일 하위 메뉴를 회수한다.
     */
    public void revoke(Long memberId, SubMenuCode subMenuCode) {
        revokeInternal(memberId, subMenuCode);
        forcedLogoutService.markLogoutNow(memberId);
    }

    /**
     * 기능에 속한 모든 하위 메뉴를 일괄 부여한다.
     */
    public void grantAll(Long memberId, FeatureCode featureCode, Long grantedBy) {
        SubMenuCode.byFeature(featureCode).forEach(sub -> grantInternal(memberId, sub, grantedBy));
    }

    /**
     * 기능에 속한 모든 하위 메뉴를 일괄 회수한다.
     */
    public void revokeAll(Long memberId, FeatureCode featureCode) {
        SubMenuCode.byFeature(featureCode).forEach(sub -> revokeInternal(memberId, sub));
    }

    private void grantInternal(Long memberId, SubMenuCode subMenuCode, Long grantedBy) {
        repository.findByMemberIdAndSubMenuCode(memberId, subMenuCode)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.activate(grantedBy);
                                repository.save(existing);
                            }
                        },
                        () -> repository.save(MemberSubMenuGrant.create(memberId, subMenuCode, grantedBy))
                );
    }

    private void revokeInternal(Long memberId, SubMenuCode subMenuCode) {
        repository.findByMemberIdAndSubMenuCode(memberId, subMenuCode)
                .ifPresent(g -> {
                    g.revoke();
                    repository.save(g);
                });
    }
}
