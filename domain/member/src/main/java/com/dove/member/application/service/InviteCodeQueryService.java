package com.dove.member.application.service;

import com.dove.member.domain.entity.InviteCode;
import com.dove.member.domain.repository.InviteCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InviteCodeQueryService {

    private final InviteCodeRepository inviteCodeRepository;

    public Optional<InviteCode> findValidCode(String code) {
        return inviteCodeRepository.findByCodeAndUsedAtIsNullAndExpiresAtAfter(code, LocalDateTime.now());
    }

    public List<InviteCode> findAll() {
        return inviteCodeRepository.findAllByOrderByCreatedAtDesc();
    }
}
