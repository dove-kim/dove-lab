package com.dove.api.global.security;

import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.auth.domain.entity.Credential;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    @Value("${init.admin.username:}") private String username;
    @Value("${init.admin.password:}") private String password;

    private final MemberProfileQueryService memberProfileQueryService;
    private final MemberProfileCommandService memberProfileCommandService;
    private final CredentialQueryService credentialQueryService;
    private final CredentialCommandService credentialCommandService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) return;

        if (!memberProfileQueryService.existsRoot()) {
            MemberProfile profile = memberProfileCommandService.save(
                    MemberProfile.create(username + "@admin.local", username, MemberRole.ROOT));
            credentialCommandService.save(
                    Credential.create(profile.getId(), username, passwordEncoder.encode(password)));
            log.info("루트 계정 생성 완료: username={}, memberId={}", username, profile.getId());
            return;
        }

        // 비밀번호 변경 감지 → 자동 갱신 (환경변수 교체 후 재시작 시 동작)
        credentialQueryService.findByUsername(username).ifPresent(cred -> {
            if (!passwordEncoder.matches(password, cred.getPasswordHash())) {
                cred.updatePasswordHash(passwordEncoder.encode(password));
                credentialCommandService.save(cred);
                log.info("루트 비밀번호 갱신 완료: username={}", username);
            }
        });
    }
}
