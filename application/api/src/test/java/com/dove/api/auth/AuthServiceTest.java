package com.dove.api.auth;

import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.CredentialQueryService;
import com.dove.auth.application.service.InviteCodeCommandService;
import com.dove.auth.application.service.InviteCodeQueryService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.api.auth.dto.LoginResult;
import com.dove.api.auth.service.AuthService;
import com.dove.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock CredentialQueryService credentialQueryService;
    @Mock CredentialCommandService credentialCommandService;
    @Mock MemberProfileQueryService memberProfileQueryService;
    @Mock MemberProfileCommandService memberProfileCommandService;
    @Mock InviteCodeQueryService inviteCodeQueryService;
    @Mock InviteCodeCommandService inviteCodeCommandService;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private static MemberProfile profileWithId(Long id, String email, String name, MemberRole role) {
        MemberProfile p = MemberProfile.create(email, name, role);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void shouldLoginWhenCredentialsValid() {
        Credential credential = Credential.create(1L, "test", "encoded");
        MemberProfile profile = profileWithId(1L, "t@t.com", "테스트", MemberRole.ADMIN);

        given(credentialQueryService.findByUsername("test")).willReturn(Optional.of(credential));
        given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
        given(memberProfileQueryService.findById(1L)).willReturn(Optional.of(profile));
        given(jwtProvider.generate(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .willReturn("jwt-token");

        LoginResult result = authService.login("test", "1234", false);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.role()).isEqualTo(MemberRole.ADMIN);
        assertThat(result.memberId()).isEqualTo(1L);
        verify(credentialCommandService).save(credential); // recordSuccessfulLogin 직후 save
    }

    @Test
    void shouldThrowWhenUsernameNotFound() {
        given(credentialQueryService.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "1234", false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldThrowWhenPasswordMismatch() {
        Credential credential = Credential.create(1L, "test", "encoded");
        given(credentialQueryService.findByUsername("test")).willReturn(Optional.of(credential));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login("test", "wrong", false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());

        verify(credentialCommandService).save(credential); // recordFailedLogin → save
    }

    @Test
    void shouldThrowWhenAccountLocked() {
        Credential credential = Credential.create(1L, "test", "encoded");
        credential.lockUntil(LocalDateTime.now().plusMinutes(10));
        given(credentialQueryService.findByUsername("test")).willReturn(Optional.of(credential));

        assertThatThrownBy(() -> authService.login("test", "1234", false))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRegisterWhenInviteCodeValid() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
        MemberProfile saved = profileWithId(42L, "new@t.com", "신규", MemberRole.USER);

        given(inviteCodeQueryService.findValidCode("valid-code")).willReturn(Optional.of(code));
        given(credentialQueryService.existsByUsername("newuser")).willReturn(false);
        given(memberProfileQueryService.existsByEmail("new@t.com")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn("encoded");
        given(memberProfileCommandService.save(any())).willReturn(saved);
        given(jwtProvider.generate(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .willReturn("jwt-token");

        LoginResult result = authService.register("valid-code", "newuser", "pass1234", "new@t.com", "신규");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.memberId()).isEqualTo(42L);
        verify(credentialCommandService).save(any(Credential.class));
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
        given(credentialQueryService.existsByUsername("dup")).willReturn(true);

        assertThatThrownBy(() -> authService.register("valid-code", "dup", "pass", "e@e.com", "이름"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
        given(inviteCodeQueryService.findValidCode("valid-code")).willReturn(Optional.of(code));
        given(credentialQueryService.existsByUsername("newuser")).willReturn(false);
        given(memberProfileQueryService.existsByEmail("dup@t.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register("valid-code", "newuser", "pass", "dup@t.com", "이름"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value());
    }
}
