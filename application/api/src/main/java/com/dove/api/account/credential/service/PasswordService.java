package com.dove.api.account.credential.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.entity.Credential;
import com.dove.api.global.security.AuthenticatedUser;
import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 본인 비밀번호 변경 use case.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final CredentialService credentialService;
    private final MemberProfileQueryService memberProfileQueryService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호를 변경하고 갱신된 JWT 토큰을 반환한다.
     */
    public String changePassword(AuthenticatedUser user, String currentPassword, String newPassword) {
        Credential credential = credentialService.findByMemberId(user.memberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (!user.mustChangePassword()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_REQUIRED");
            }
            if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
            }
        }

        credential.changePassword(passwordEncoder.encode(newPassword));
        credentialService.save(credential);

        MemberProfile profile = memberProfileQueryService.findById(user.memberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        return jwtProvider.generateAccessToken(
                profile.getId(), credential.getUsername(), profile.getName(),
                profile.getRole().name(), false, user.capabilities());
    }
}
