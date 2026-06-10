package com.dove.api.account.auth.service;

import com.dove.api.account.auth.dto.LoginResult;
import com.dove.auth.application.service.CredentialService;
import com.dove.auth.application.service.InviteCodeService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.userfeature.application.service.MemberFeatureGrantQueryService;
import com.dove.userfeature.domain.enums.FeatureCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 로그인/회원가입/토큰 재발급 use case.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredentialService credentialService;
    private final MemberProfileQueryService memberProfileQueryService;
    private final MemberProfileCommandService memberProfileCommandService;
    private final InviteCodeService inviteCodeService;
    private final MemberFeatureGrantQueryService memberFeatureGrantQueryService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 아이디/비밀번호를 검증하고 로그인 결과(토큰·프로필)를 반환한다.
     */
    @Transactional
    public LoginResult login(String username, String password, boolean rememberMe) {
        Credential credential = credentialService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (credential.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED");
        }

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            credential.recordFailedLogin();
            credentialService.save(credential);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        MemberProfile profile = memberProfileQueryService.findById(credential.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PROFILE_NOT_FOUND"));

        boolean mustChangePassword = credential.isPasswordResetRequired();
        credential.recordSuccessfulLogin();
        credentialService.save(credential);

        Set<String> features = loadFeatureNames(profile.getId(), profile.getRole());
        String accessToken = jwtProvider.generateAccessToken(
                profile.getId(), credential.getUsername(), profile.getName(),
                profile.getRole().name(), mustChangePassword, features);
        String refreshToken = jwtProvider.generateRefreshToken(profile.getId());
        return new LoginResult(accessToken, refreshToken, profile.getId(), credential.getUsername(),
                profile.getName(), profile.getRole(), rememberMe, mustChangePassword);
    }

    /**
     * 초대 코드를 검증하고 신규 회원을 생성한 뒤 로그인 결과를 반환한다.
     */
    @Transactional
    public LoginResult register(String inviteCode, String username, String password, String email, String name) {
        InviteCode code = inviteCodeService.findValidCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID"));

        if (credentialService.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_DUPLICATE");
        }
        if (memberProfileQueryService.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_DUPLICATE");
        }

        MemberProfile profile = memberProfileCommandService.save(
                MemberProfile.create(email, name, code.getRole()));
        credentialService.save(
                Credential.create(profile.getId(), username, passwordEncoder.encode(password)));
        inviteCodeService.use(code);

        Set<String> features = loadFeatureNames(profile.getId(), profile.getRole());
        String accessToken = jwtProvider.generateAccessToken(
                profile.getId(), username, name, profile.getRole().name(), false, features);
        String refreshToken = jwtProvider.generateRefreshToken(profile.getId());
        return new LoginResult(accessToken, refreshToken, profile.getId(), username, name,
                profile.getRole(), false, false);
    }

    /**
     * access/refresh 한 쌍을 재발급한다 (rolling refresh).
     */
    @Transactional(readOnly = true)
    public LoginResult refresh(Long memberId) {
        MemberProfile profile = memberProfileQueryService.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PROFILE_NOT_FOUND"));
        Credential credential = credentialService.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        if (credential.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED");
        }
        boolean mustChangePassword = credential.isPasswordResetRequired();

        Set<String> features = loadFeatureNames(memberId, profile.getRole());
        String accessToken = jwtProvider.generateAccessToken(
                profile.getId(), credential.getUsername(), profile.getName(),
                profile.getRole().name(), mustChangePassword, features);
        String refreshToken = jwtProvider.generateRefreshToken(profile.getId());
        return new LoginResult(accessToken, refreshToken, profile.getId(), credential.getUsername(),
                profile.getName(), profile.getRole(), false, mustChangePassword);
    }

    private Set<String> loadFeatureNames(Long memberId, MemberRole role) {
        if (role == MemberRole.ROOT) {
            return Arrays.stream(FeatureCode.values())
                    .map(FeatureCode::name)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return memberFeatureGrantQueryService.findActiveFeatureCodes(memberId).stream()
                .map(FeatureCode::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
