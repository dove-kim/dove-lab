package com.dove.api.account.credential.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.infrastructure.security.TemporaryPasswordGenerator;
import com.dove.auth.domain.entity.Credential;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 관리자에 의한 회원 비밀번호 초기화 use case.
 */
@Service
@RequiredArgsConstructor
public class AdminPasswordResetService {

    private final CredentialService credentialService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordEncoder passwordEncoder;

    /**
     * 임시 비밀번호를 생성해 저장하고, 평문 임시 비밀번호를 반환한다.
     */
    public String resetPassword(Long userId) {
        Credential credential = credentialService.findByMemberId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        String tempPassword = temporaryPasswordGenerator.generate();
        credential.resetPassword(passwordEncoder.encode(tempPassword));
        credentialService.save(credential);
        return tempPassword;
    }
}
