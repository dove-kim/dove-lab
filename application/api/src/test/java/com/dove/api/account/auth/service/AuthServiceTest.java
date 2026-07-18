package com.dove.api.account.auth.service;

import com.dove.auth.application.service.CredentialService;
import com.dove.auth.application.service.InviteCodeService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.api.account.auth.dto.LoginResult;
import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.application.service.MemberProfileQueryService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.userfeature.application.service.MemberCapabilityGrantQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.Set;

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

    @Mock CredentialService credentialService;
    @Mock MemberProfileQueryService memberProfileQueryService;
    @Mock MemberProfileCommandService memberProfileCommandService;
    @Mock InviteCodeService inviteCodeService;
    @Mock MemberCapabilityGrantQueryService memberCapabilityGrantQueryService;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private static MemberProfile profileWithId(Long id, String email, String name, MemberRole role) {
        MemberProfile p = MemberProfile.create(email, name, role);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("자격증명이 유효하면 로그인 성공")
        void shouldLoginWhenCredentialsValid() {
            Credential credential = Credential.create(1L, "test", "encoded");
            MemberProfile profile = profileWithId(1L, "t@t.com", "테스트", MemberRole.ADMIN);

            given(credentialService.findByUsername("test")).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
            given(memberProfileQueryService.findById(1L)).willReturn(Optional.of(profile));
            given(memberCapabilityGrantQueryService.findGrantedCapabilities(1L)).willReturn(Set.of());
            given(jwtProvider.generateAccessToken(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), any()))
                    .willReturn("jwt-access");
            given(jwtProvider.generateRefreshToken(anyLong())).willReturn("jwt-refresh");

            LoginResult result = authService.login("test", "1234", false);

            assertThat(result.accessToken()).isEqualTo("jwt-access");
            assertThat(result.refreshToken()).isEqualTo("jwt-refresh");
            assertThat(result.role()).isEqualTo(MemberRole.ADMIN);
            assertThat(result.memberId()).isEqualTo(1L);
            verify(credentialService).save(credential);
        }

        @Test
        @DisplayName("아이디가 없으면 401 예외")
        void shouldThrowWhenUsernameNotFound() {
            given(credentialService.findByUsername("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login("unknown", "1234", false))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 401 예외")
        void shouldThrowWhenPasswordMismatch() {
            Credential credential = Credential.create(1L, "test", "encoded");
            given(credentialService.findByUsername("test")).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

            assertThatThrownBy(() -> authService.login("test", "wrong", false))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());

            verify(credentialService).save(credential);
        }

        @Test
        @DisplayName("계정이 잠겨 있으면 401 예외")
        void shouldThrowWhenAccountLocked() {
            Credential credential = Credential.create(1L, "test", "encoded");
            credential.lockUntil(LocalDateTime.now().plusMinutes(10));
            given(credentialService.findByUsername("test")).willReturn(Optional.of(credential));

            assertThatThrownBy(() -> authService.login("test", "1234", false))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("탈퇴한 계정이면 401 예외")
        void shouldThrowWhenAccountDeleted() {
            Credential credential = Credential.create(1L, "test", "encoded");
            MemberProfile profile = profileWithId(1L, "t@t.com", "테스트", MemberRole.USER);
            profile.softDelete();

            given(credentialService.findByUsername("test")).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("1234", "encoded")).willReturn(true);
            given(memberProfileQueryService.findById(1L)).willReturn(Optional.of(profile));

            assertThatThrownBy(() -> authService.login("test", "1234", false))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("초대 코드가 유효하면 회원 가입 성공")
        void shouldRegisterWhenInviteCodeValid() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
            MemberProfile saved = profileWithId(42L, "new@t.com", "신규", MemberRole.USER);

            given(inviteCodeService.findValidCode("valid-code")).willReturn(Optional.of(code));
            given(credentialService.existsByUsername("newuser")).willReturn(false);
            given(memberProfileQueryService.existsByEmail("new@t.com")).willReturn(false);
            given(passwordEncoder.encode("pass1234")).willReturn("encoded");
            given(memberProfileCommandService.save(any())).willReturn(saved);
            given(memberCapabilityGrantQueryService.findGrantedCapabilities(42L)).willReturn(Set.of());
            given(jwtProvider.generateAccessToken(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), any()))
                    .willReturn("jwt-access");
            given(jwtProvider.generateRefreshToken(anyLong())).willReturn("jwt-refresh");

            LoginResult result = authService.register("valid-code", "newuser", "pass1234", "new@t.com", "신규");

            assertThat(result.accessToken()).isEqualTo("jwt-access");
            assertThat(result.refreshToken()).isEqualTo("jwt-refresh");
            assertThat(result.memberId()).isEqualTo(42L);
            verify(credentialService).save(any(Credential.class));
            verify(inviteCodeService).use(code);
        }

        @Test
        @DisplayName("초대 코드가 유효하지 않으면 400 예외")
        void shouldThrowWhenInviteCodeInvalid() {
            given(inviteCodeService.findValidCode("bad-code")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register("bad-code", "user", "pass", "e@e.com", "이름"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("아이디가 이미 존재하면 409 예외")
        void shouldThrowWhenUsernameAlreadyExists() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
            given(inviteCodeService.findValidCode("valid-code")).willReturn(Optional.of(code));
            given(credentialService.existsByUsername("dup")).willReturn(true);

            assertThatThrownBy(() -> authService.register("valid-code", "dup", "pass", "e@e.com", "이름"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("이메일이 이미 존재하면 409 예외")
        void shouldThrowWhenEmailAlreadyExists() {
            InviteCode code = InviteCode.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "admin");
            given(inviteCodeService.findValidCode("valid-code")).willReturn(Optional.of(code));
            given(credentialService.existsByUsername("newuser")).willReturn(false);
            given(memberProfileQueryService.existsByEmail("dup@t.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register("valid-code", "newuser", "pass", "dup@t.com", "이름"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                    .isEqualTo(HttpStatus.CONFLICT.value());
        }
    }
}
