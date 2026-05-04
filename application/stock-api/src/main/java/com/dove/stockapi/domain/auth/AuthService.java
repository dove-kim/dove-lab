package com.dove.stockapi.domain.auth;

import com.dove.member.application.service.InviteCodeCommandService;
import com.dove.member.application.service.InviteCodeQueryService;
import com.dove.member.application.service.MemberCommandService;
import com.dove.member.application.service.MemberQueryService;
import com.dove.member.domain.entity.InviteCode;
import com.dove.member.domain.entity.Member;
import com.dove.member.domain.entity.MemberRole;
import com.dove.stockapi.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    private final InviteCodeQueryService inviteCodeQueryService;
    private final InviteCodeCommandService inviteCodeCommandService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public record LoginResult(String accessToken, String username, String name, MemberRole role, boolean rememberMe) {}

    public LoginResult login(String username, String password, boolean rememberMe) {
        Member member = memberQueryService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        String token = jwtProvider.generate(member.getUsername(), member.getName(), member.getRole(), rememberMe);
        return new LoginResult(token, member.getUsername(), member.getName(), member.getRole(), rememberMe);
    }

    @Transactional
    public LoginResult register(String inviteCode, String username, String password, String email, String name) {
        InviteCode code = inviteCodeQueryService.findValidCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVITE_CODE_INVALID"));

        if (memberQueryService.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_DUPLICATE");
        }
        if (memberQueryService.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_DUPLICATE");
        }

        Member member = Member.create(username, passwordEncoder.encode(password), email, name, code.getRole());
        memberCommandService.save(member);
        inviteCodeCommandService.use(code);

        String token = jwtProvider.generate(member.getUsername(), member.getName(), member.getRole(), false);
        return new LoginResult(token, member.getUsername(), member.getName(), member.getRole(), false);
    }
}
