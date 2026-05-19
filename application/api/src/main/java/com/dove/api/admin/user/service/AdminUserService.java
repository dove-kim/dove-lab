package com.dove.api.admin.user.service;

import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.auth.domain.entity.Credential;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CredentialQueryService credentialQueryService;
    private final CredentialCommandService credentialCommandService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 임시 비밀번호를 생성해 저장하고, 평문 임시 비밀번호를 반환한다.
     */
    public String resetPassword(Long userId) {
        Credential credential = credentialQueryService.findByMemberId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        String tempPassword = generateTemporaryPassword();
        credential.resetPassword(passwordEncoder.encode(tempPassword));
        credentialCommandService.save(credential);
        return tempPassword;
    }

    private static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(TEMP_PW_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }
}
