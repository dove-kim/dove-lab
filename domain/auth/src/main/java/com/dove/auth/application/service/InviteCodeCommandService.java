package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.repository.InviteCodeRepository;
import com.dove.user.domain.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class InviteCodeCommandService {

    private final InviteCodeRepository inviteCodeRepository;

    public InviteCode create(MemberRole role, LocalDateTime expiresAt, String createdBy) {
        InviteCode code = InviteCode.create(role, expiresAt, createdBy);
        return inviteCodeRepository.save(code);
    }

    public void use(InviteCode inviteCode) {
        inviteCode.use();
        inviteCodeRepository.save(inviteCode);
    }
}
