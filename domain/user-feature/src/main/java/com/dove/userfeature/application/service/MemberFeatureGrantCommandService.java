package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.MemberFeatureDisplay;
import com.dove.userfeature.domain.entity.MemberFeatureGrant;
import com.dove.userfeature.domain.entity.MemberModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.userfeature.domain.repository.MemberFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.MemberFeatureGrantRepository;
import com.dove.userfeature.domain.repository.MemberModuleDisplayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 기능 부여·회수 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberFeatureGrantCommandService {

    private final MemberFeatureGrantRepository grantRepository;
    private final MemberFeatureDisplayRepository featureDisplayRepository;
    private final MemberModuleDisplayRepository moduleDisplayRepository;
    private final MemberSubMenuGrantCommandService subMenuGrantCommandService;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 사용자에게 기능을 부여하고 소속 하위 메뉴를 함께 부여한다.
     */
    public void grant(Long memberId, FeatureCode featureCode, Long grantedBy) {
        grantRepository.findByMemberIdAndFeatureCode(memberId, featureCode)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.activate(grantedBy);
                                grantRepository.save(existing);
                                ensureDisplayExists(memberId, featureCode);
                                subMenuGrantCommandService.grantAll(memberId, featureCode, grantedBy);
                            }
                        },
                        () -> {
                            grantRepository.save(MemberFeatureGrant.create(memberId, featureCode, grantedBy));
                            ensureDisplayExists(memberId, featureCode);
                            subMenuGrantCommandService.grantAll(memberId, featureCode, grantedBy);
                        }
                );
        forcedLogoutService.markLogoutNow(memberId);
    }

    /**
     * 사용자의 기능을 회수하고 소속 하위 메뉴도 함께 회수한다.
     */
    public void revoke(Long memberId, FeatureCode featureCode) {
        grantRepository.findByMemberIdAndFeatureCode(memberId, featureCode)
                .ifPresent(grant -> {
                    grant.revoke();
                    grantRepository.save(grant);
                    subMenuGrantCommandService.revokeAll(memberId, featureCode);
                });
        forcedLogoutService.markLogoutNow(memberId);
    }

    /**
     * 기능·모듈 표시 설정이 없으면 생성한다.
     */
    private void ensureDisplayExists(Long memberId, FeatureCode featureCode) {
        featureDisplayRepository.findByMemberIdAndFeatureCode(memberId, featureCode)
                .ifPresentOrElse(
                        existing -> { /* 이미 있으면 유지 (기존 순서 복원) */ },
                        () -> {
                            List<MemberFeatureDisplay> existing = featureDisplayRepository.findAllByMemberId(memberId);
                            ModuleCode module = featureCode.getModule();
                            int nextOrder = existing.stream()
                                    .filter(d -> d.getFeatureCode().getModule() == module)
                                    .mapToInt(MemberFeatureDisplay::getDisplayOrder)
                                    .max()
                                    .orElse(-1) + 1;
                            featureDisplayRepository.save(
                                    MemberFeatureDisplay.create(memberId, featureCode, nextOrder));
                        }
                );

        moduleDisplayRepository.findByMemberIdAndModuleCode(memberId, featureCode.getModule())
                .ifPresentOrElse(
                        existing -> { /* 이미 있으면 유지 */ },
                        () -> {
                            int nextModuleOrder = moduleDisplayRepository.findAllByMemberId(memberId).stream()
                                    .mapToInt(MemberModuleDisplay::getDisplayOrder)
                                    .max()
                                    .orElse(-1) + 1;
                            moduleDisplayRepository.save(
                                    MemberModuleDisplay.create(memberId, featureCode.getModule(), nextModuleOrder));
                        }
                );
    }
}
