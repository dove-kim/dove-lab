package com.dove.stockapi.global.security;

import com.dove.member.application.service.MemberCommandService;
import com.dove.member.application.service.MemberQueryService;
import com.dove.member.domain.entity.Member;
import com.dove.member.domain.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    @Value("${init.admin.username:}") private String username;
    @Value("${init.admin.password:}") private String password;

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) return;
        if (memberQueryService.existsAdmin()) return;

        memberCommandService.save(Member.create(
                username,
                passwordEncoder.encode(password),
                username + "@admin.local",
                username,
                MemberRole.ADMIN));
        log.info("초기 어드민 계정 생성 완료: {}", username);
    }
}
