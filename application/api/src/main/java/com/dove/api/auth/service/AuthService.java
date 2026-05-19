package com.dove.api.auth.service;

import com.dove.api.auth.dto.LoginResult;
import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.auth.application.service.InviteCodeCommandService;
import com.dove.auth.application.service.InviteCodeQueryService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인/회원가입 use case orchestrator.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredentialQueryService credentialQueryService;
    private final CredentialCommandService credentialCommandService;
    private final MemberProfileQueryService memberProfileQueryService;
    private final MemberProfileCommandService memberProfileCommandService;
    private final InviteCodeQueryService inviteCodeQueryService;
    private final InviteCodeCommandService inviteCodeCommandService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResult login(String username, String password, boolean rememberMe) {
        Credential credential = credentialQueryService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (credential.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED");
        }

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            credential.recordFailedLogin();
            credentialCommandService.save(credential);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        MemberProfile profile = memberProfileQueryService.findById(credential.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PROFILE_NOT_FOUND"));

        boolean mustChangePassword = credential.isPasswordResetRequired();
        credential.recordSuccessfulLogin();
        credentialCommandService.save(credential);

        String token = jwtProvider.generate(
                profile.getId(), credential.getUsername(), profile.getName(),
                profile.getRole().name(), rememberMe, mustChangePassword);
        return new LoginResult(token, profile.getId(), credential.getUsername(),
                profile.getName(), profile.getRole(), rememberMe, mustChangePassword);
    }

    @Transactional
    public LoginResult register(String inviteCode, String username, String password, String email, String name) {
        InviteCode code = inviteCodeQueryService.findValidCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID"));

        if (credentialQueryService.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_DUPLICATE");
        }
        if (memberProfileQueryService.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_DUPLICATE");
        }

        MemberProfile profile = memberProfileCommandService.save(
                MemberProfile.create(email, name, code.getRole()));
        credentialCommandService.save(
                Credential.create(profile.getId(), username, passwordEncoder.encode(password)));
        inviteCodeCommandService.use(code);

        String token = jwtProvider.generate(
                profile.getId(), username, name, profile.getRole().name(), false, false);
        return new LoginResult(token, profile.getId(), username, name, profile.getRole(), false, false);
    }
}
