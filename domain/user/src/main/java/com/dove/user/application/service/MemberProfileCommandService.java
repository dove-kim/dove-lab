package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileCommandService {

    private final MemberProfileRepository memberProfileRepository;

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
        return memberProfileRepository.save(profile);
    }
}
