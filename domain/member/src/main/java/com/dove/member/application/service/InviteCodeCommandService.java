package com.dove.member.application.service;

import com.dove.member.domain.entity.InviteCode;
import com.dove.member.domain.entity.MemberRole;
import com.dove.member.domain.repository.InviteCodeRepository;
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
        return inviteCodeRepository.save(InviteCode.create(role, expiresAt, createdBy));
    }

    public void use(InviteCode inviteCode) {
        inviteCode.use();
    }
}
