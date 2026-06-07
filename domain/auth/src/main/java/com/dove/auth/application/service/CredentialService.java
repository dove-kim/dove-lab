package com.dove.auth.application.service;

import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 회원 자격증명을 저장·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CredentialService {

    private final CredentialRepository credentialRepository;

    public Credential save(Credential credential) {
        return credentialRepository.save(credential);
    }

    @Transactional(readOnly = true)
    public Optional<Credential> findByUsername(String username) {
        return credentialRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Credential> findByMemberId(Long memberId) {
        return credentialRepository.findById(memberId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return credentialRepository.existsByUsername(username);
    }

    /**
     * memberId 목록에 대한 username 맵 반환 (memberId → username).
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findUsernamesByMemberIds(Collection<Long> memberIds) {
        return credentialRepository.findAllByMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(Credential::getMemberId, Credential::getUsername));
    }
}
