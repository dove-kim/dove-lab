package com.dove.auth.application.service;

import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CredentialQueryService {

    private final CredentialRepository credentialRepository;

    public Optional<Credential> findByUsername(String username) {
        return credentialRepository.findByUsername(username);
    }

    public Optional<Credential> findByMemberId(Long memberId) {
        return credentialRepository.findById(memberId);
    }

    public boolean existsByUsername(String username) {
        return credentialRepository.existsByUsername(username);
    }

    /** memberId 목록에 대한 username 맵 반환 (memberId → username). */
    public Map<Long, String> findUsernamesByMemberIds(Collection<Long> memberIds) {
        return credentialRepository.findAllByMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(Credential::getMemberId, Credential::getUsername));
    }
}
