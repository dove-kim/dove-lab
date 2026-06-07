package com.dove.auth.application.service;

import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.auth.domain.repository.InviteCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 초대 코드를 발급·사용·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InviteCodeService {

    private final InviteCodeRepository inviteCodeRepository;

    /**
     * 지정 역할·만료일시로 새 초대 코드를 발급한다.
     */
    public InviteCode create(MemberRole role, LocalDateTime expiresAt, String createdBy) {
        InviteCode code = InviteCode.create(role, expiresAt, createdBy);
        return inviteCodeRepository.save(code);
    }

    /**
     * 초대 코드를 사용 처리한다.
     */
    public void use(InviteCode inviteCode) {
        inviteCode.use();
        inviteCodeRepository.save(inviteCode);
    }

    /**
     * 코드 값으로 유효한(미사용·미만료) 초대 코드를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<InviteCode> findValidCode(String code) {
        return inviteCodeRepository.findByCode(code).filter(InviteCode::isValid);
    }

    /**
     * 모든 초대 코드를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<InviteCode> findAll() {
        return inviteCodeRepository.findAll();
    }
}
