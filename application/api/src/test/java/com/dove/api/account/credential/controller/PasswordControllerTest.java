package com.dove.api.account.credential.controller;

import com.dove.api.TestApiApplication;
import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.infrastructure.security.JwtProvider;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PasswordControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired CredentialService credentialService;
    @Autowired JwtProvider jwtProvider;
    @Autowired PasswordEncoder passwordEncoder;

    private String normalToken;   // mustChangePassword = false
    private String forcedToken;   // mustChangePassword = true

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("pw@test.com", "비번유저", MemberRole.USER));
        credentialService.save(
                Credential.create(user.getId(), "pwuser", passwordEncoder.encode("current1234")));
        normalToken = jwtProvider.generateAccessToken(
                user.getId(), "pwuser", user.getName(), user.getRole().name(), false, Set.of());
        forcedToken = jwtProvider.generateAccessToken(
                user.getId(), "pwuser", user.getName(), user.getRole().name(), true, Set.of());
    }

    @Nested
    @DisplayName("PATCH /account/password")
    class ChangePassword {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"current1234","newPassword":"new1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("현재 비밀번호 일치 시 200 + 새 토큰 반환")
        void shouldChangePasswordAndReturnNewToken() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .header("Authorization", "Bearer " + normalToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"current1234","newPassword":"new1234"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("mustChangePassword=true 이면 현재 비밀번호 없이 200")
        void shouldChangePasswordWithoutCurrentPasswordWhenForced() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .header("Authorization", "Bearer " + forcedToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"forced1234"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("현재 비밀번호 틀리면 401")
        void shouldReturn401WhenCurrentPasswordWrong() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .header("Authorization", "Bearer " + normalToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"wrong","newPassword":"new1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("현재 비밀번호 누락 시 400")
        void shouldReturn400WhenCurrentPasswordMissing() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .header("Authorization", "Bearer " + normalToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword":"new1234"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("새 비밀번호 너무 짧으면 400 (@Valid)")
        void shouldReturn400WhenNewPasswordTooShort() throws Exception {
            mockMvc.perform(patch("/account/password")
                            .header("Authorization", "Bearer " + normalToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"current1234","newPassword":"ab"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}
