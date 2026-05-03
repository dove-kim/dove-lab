package com.dove.stockapi.domain.auth;

import com.dove.member.application.service.InviteCodeCommandService;
import com.dove.member.application.service.InviteCodeQueryService;
import com.dove.member.application.service.MemberCommandService;
import com.dove.member.application.service.MemberQueryService;
import com.dove.member.domain.entity.InviteCode;
import com.dove.member.domain.entity.Member;
import com.dove.member.domain.entity.MemberRole;
import com.dove.stockapi.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock MemberQueryService memberQueryService;
    @Mock MemberCommandService memberCommandService;
    @Mock InviteCodeQueryService inviteCodeQueryService;
    @Mock InviteCodeCommandService inviteCodeCommandService;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    @Test
    void shouldLoginWhenCredentialsValid() {
        Member member = Member.create("test", "encoded", "t@t.com", "테스트", MemberRole.ADMIN);
        given(memberQueryService.findByUsername("test")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
        given(jwtProvider.generate(anyString(), anyString(), any(), anyBoolean())).willReturn("jwt-token");

        AuthService.LoginResult result = authService.login("test", "1234", false);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.role()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void shouldThrowWhenUsernameNotFound() {
        given(memberQueryService.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "1234", false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldThrowWhenPasswordMismatch() {
        Member member = Member.create("test", "encoded", "t@t.com", "테스트", MemberRole.USER);
        given(memberQueryService.findByUsername("test")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login("test", "wrong", false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldRegisterWhenInviteCodeValid() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
        Member saved = Member.create("newuser", "encoded", "new@t.com", "신규", MemberRole.USER);

        given(inviteCodeQueryService.findValidCode("valid-code")).willReturn(Optional.of(code));
        given(memberQueryService.existsByUsername("newuser")).willReturn(false);
        given(memberQueryService.existsByEmail("new@t.com")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn("encoded");
        given(memberCommandService.save(any())).willReturn(saved);
        given(jwtProvider.generate(anyString(), anyString(), any(), anyBoolean())).willReturn("jwt-token");

        AuthService.LoginResult result = authService.register("valid-code", "newuser", "pass1234", "new@t.com", "신규");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        verify(inviteCodeCommandService).use(code);
    }

    @Test
    void shouldThrowWhenInviteCodeInvalid() {
        given(inviteCodeQueryService.findValidCode("bad-code")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register("bad-code", "user", "pass", "e@e.com", "이름"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
        given(inviteCodeQueryService.findValidCode("valid-code")).willReturn(Optional.of(code));
        given(memberQueryService.existsByUsername("dup")).willReturn(true);

        assertThatThrownBy(() -> authService.register("valid-code", "dup", "pass", "e@e.com", "이름"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
        given(inviteCodeQueryService.findValidCode("valid-code")).willReturn(Optional.of(code));
        given(memberQueryService.existsByUsername("newuser")).willReturn(false);
        given(memberQueryService.existsByEmail("dup@t.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register("valid-code", "newuser", "pass", "dup@t.com", "이름"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }
}
