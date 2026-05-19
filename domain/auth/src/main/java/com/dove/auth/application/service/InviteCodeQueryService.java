package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.repository.InviteCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InviteCodeQueryService {

    private final InviteCodeRepository inviteCodeRepository;

    public Optional<InviteCode> findValidCode(String code) {
        return inviteCodeRepository.findByCode(code).filter(InviteCode::isValid);
    }

    public List<InviteCode> findAll() {
        return inviteCodeRepository.findAll();
    }
}
