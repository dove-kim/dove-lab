package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 회원 프로필 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileQueryService {

    private final MemberProfileRepository memberProfileRepository;

    /**
     * 전체 회원을 가입일시 내림차순으로 반환한다.
     */
    public List<MemberProfile> findAll() {
        return memberProfileRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 회원 ID로 프로필을 조회한다.
     */
    public Optional<MemberProfile> findById(Long id) {
        return memberProfileRepository.findById(id);
    }

    /**
     * 해당 이메일을 가진 회원이 존재하는지 여부를 반환한다.
     */
    public boolean existsByEmail(String email) {
        return memberProfileRepository.existsByEmail(email);
    }

    /**
     * ROOT 역할 회원이 존재하는지 여부를 반환한다.
     */
    public boolean existsRoot() {
        return memberProfileRepository.existsByRole(MemberRole.ROOT);
    }
}
