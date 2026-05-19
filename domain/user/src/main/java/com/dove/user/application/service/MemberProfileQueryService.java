package com.dove.user.application.service;

import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import com.dove.user.domain.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileQueryService {

    private final MemberProfileRepository memberProfileRepository;

    public List<MemberProfile> findAll() {
        return memberProfileRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<MemberProfile> findById(Long id) {
        return memberProfileRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return memberProfileRepository.existsByEmail(email);
    }

    public boolean existsAdmin() {
        return memberProfileRepository.existsByRole(MemberRole.ADMIN);
    }

    public boolean existsRoot() {
        return memberProfileRepository.existsByRole(MemberRole.ROOT);
    }
}
