package com.dove.user.application.service;

import com.dove.auth.application.service.ForcedLogoutService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 회원 프로필 저장·역할 변경 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileCommandService {

    private final MemberProfileRepository memberProfileRepository;
    private final ForcedLogoutService forcedLogoutService;

    /**
     * 새 프로필을 저장하고 ID가 채번된 인스턴스를 반환한다.
     *
     * @param profile 저장할 프로필 (id == null 권장)
     * @return id가 채번된 영속 프로필
     */
    public MemberProfile save(MemberProfile profile) {
        return memberProfileRepository.save(profile);
    }

    /**
     * 지정 회원의 역할을 변경한다.
     *
     * @param userId  대상 회원 ID
     * @param newRole 변경할 역할
     * @return 변경 후 저장된 프로필
     * @throws NoSuchElementException 회원이 존재하지 않을 경우
     */
    public MemberProfile changeRole(Long userId, MemberRole newRole) {
        MemberProfile profile = memberProfileRepository.findById(userId)
                .orElseThrow(NoSuchElementException::new);
        if (profile.getRole() == MemberRole.ROOT) {
            throw new IllegalStateException("ROOT_ROLE_IMMUTABLE");
        }
        if (newRole == MemberRole.ROOT) {
            throw new IllegalArgumentException("ROOT_ROLE_IMMUTABLE");
        }
        profile.changeRole(newRole);
        MemberProfile saved = memberProfileRepository.save(profile);
        forcedLogoutService.markLogoutNow(userId);
        return saved;
    }

    /**
     * 지정 회원을 탈퇴 처리한다(soft delete). 행은 유지되어 참조 무결성이 보존되고, 활성 세션은 강제 로그아웃된다.
     *
     * @param userId 대상 회원 ID
     * @throws NoSuchElementException 회원이 존재하지 않을 경우
     * @throws IllegalStateException  ROOT 회원을 삭제하려 할 경우
     */
    public void softDelete(Long userId) {
        MemberProfile profile = memberProfileRepository.findById(userId)
                .orElseThrow(NoSuchElementException::new);
        if (profile.getRole() == MemberRole.ROOT) {
            throw new IllegalStateException("ROOT_CANNOT_BE_DELETED");
        }
        if (profile.isDeleted()) {
            return;
        }
        profile.softDelete();
        memberProfileRepository.save(profile);
        forcedLogoutService.markLogoutNow(userId);
    }
}
