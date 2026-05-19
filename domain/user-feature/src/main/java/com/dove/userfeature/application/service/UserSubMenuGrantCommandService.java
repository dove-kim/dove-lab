package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserSubMenuGrant;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.SubMenuCode;
import com.dove.userfeature.domain.repository.UserSubMenuGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSubMenuGrantCommandService {

    private final UserSubMenuGrantRepository repository;

    /** 단일 하위 메뉴 부여. */
    public void grant(Long userId, SubMenuCode subMenuCode, Long grantedBy) {
        repository.findByUserIdAndSubMenuCode(userId, subMenuCode)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.activate(grantedBy);
                                repository.save(existing);
                            }
                        },
                        () -> repository.save(UserSubMenuGrant.create(userId, subMenuCode, grantedBy))
                );
    }

    /** 단일 하위 메뉴 회수. */
    public void revoke(Long userId, SubMenuCode subMenuCode) {
        repository.findByUserIdAndSubMenuCode(userId, subMenuCode)
                .ifPresent(g -> {
                    g.revoke();
                    repository.save(g);
                });
    }

    /** 기능에 속한 모든 하위 메뉴 일괄 부여 — 기능 부여 시 자동 호출. */
    public void grantAll(Long userId, FeatureCode featureCode, Long grantedBy) {
        SubMenuCode.byFeature(featureCode).forEach(sub -> grant(userId, sub, grantedBy));
    }

    /** 기능에 속한 모든 하위 메뉴 일괄 회수 — 기능 회수 시 자동 호출. */
    public void revokeAll(Long userId, FeatureCode featureCode) {
        SubMenuCode.byFeature(featureCode).forEach(sub -> revoke(userId, sub));
    }
}
