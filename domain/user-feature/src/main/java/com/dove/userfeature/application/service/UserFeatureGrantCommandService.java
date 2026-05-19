package com.dove.userfeature.application.service;

import com.dove.userfeature.domain.entity.UserFeatureDisplay;
import com.dove.userfeature.domain.entity.UserFeatureGrant;
import com.dove.userfeature.domain.entity.UserModuleDisplay;
import com.dove.userfeature.domain.enums.FeatureCode;
import com.dove.userfeature.domain.enums.ModuleCode;
import com.dove.userfeature.domain.repository.UserFeatureDisplayRepository;
import com.dove.userfeature.domain.repository.UserFeatureGrantRepository;
import com.dove.userfeature.domain.repository.UserModuleDisplayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFeatureGrantCommandService {

    private final UserFeatureGrantRepository grantRepository;
    private final UserFeatureDisplayRepository featureDisplayRepository;
    private final UserModuleDisplayRepository moduleDisplayRepository;
    private final UserSubMenuGrantCommandService subMenuGrantCommandService;

    /**
     * 사용자에게 기능을 부여한다.
     *
     * <ul>
     *   <li>이미 활성 상태이면 아무 변경 없음.</li>
     *   <li>회수됐던 기능은 재활성화하며 기존 표시 설정을 유지한다.</li>
     *   <li>신규 기능이면 표시 설정을 자동 생성한다 (순서 = 기존 마지막 + 1).</li>
     *   <li>소속 하위 메뉴를 자동으로 함께 부여한다.</li>
     * </ul>
     */
    public void grant(Long userId, FeatureCode featureCode, Long grantedBy) {
        grantRepository.findByUserIdAndFeatureCode(userId, featureCode)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.activate(grantedBy);
                                grantRepository.save(existing);
                                ensureDisplayExists(userId, featureCode);
                                subMenuGrantCommandService.grantAll(userId, featureCode, grantedBy);
                            }
                        },
                        () -> {
                            grantRepository.save(UserFeatureGrant.create(userId, featureCode, grantedBy));
                            ensureDisplayExists(userId, featureCode);
                            subMenuGrantCommandService.grantAll(userId, featureCode, grantedBy);
                        }
                );
    }

    /**
     * 사용자의 기능을 회수한다.
     *
     * <p>소속 하위 메뉴도 함께 회수된다.
     */
    public void revoke(Long userId, FeatureCode featureCode) {
        grantRepository.findByUserIdAndFeatureCode(userId, featureCode)
                .ifPresent(grant -> {
                    grant.revoke();
                    grantRepository.save(grant);
                    subMenuGrantCommandService.revokeAll(userId, featureCode);
                });
    }

    /** 기능 표시 설정이 없으면 생성한다. 모듈 설정도 없으면 함께 생성한다. */
    private void ensureDisplayExists(Long userId, FeatureCode featureCode) {
        featureDisplayRepository.findByUserIdAndFeatureCode(userId, featureCode)
                .ifPresentOrElse(
                        existing -> { /* 이미 있으면 유지 (기존 순서 복원) */ },
                        () -> {
                            List<UserFeatureDisplay> existing = featureDisplayRepository.findAllByUserId(userId);
                            ModuleCode module = featureCode.getModule();
                            int nextOrder = existing.stream()
                                    .filter(d -> d.getFeatureCode().getModule() == module)
                                    .mapToInt(UserFeatureDisplay::getDisplayOrder)
                                    .max()
                                    .orElse(-1) + 1;
                            featureDisplayRepository.save(
                                    UserFeatureDisplay.create(userId, featureCode, nextOrder));
                        }
                );

        moduleDisplayRepository.findByUserIdAndModuleCode(userId, featureCode.getModule())
                .ifPresentOrElse(
                        existing -> { /* 이미 있으면 유지 */ },
                        () -> {
                            int nextModuleOrder = moduleDisplayRepository.findAllByUserId(userId).stream()
                                    .mapToInt(UserModuleDisplay::getDisplayOrder)
                                    .max()
                                    .orElse(-1) + 1;
                            moduleDisplayRepository.save(
                                    UserModuleDisplay.create(userId, featureCode.getModule(), nextModuleOrder));
                        }
                );
    }
}
