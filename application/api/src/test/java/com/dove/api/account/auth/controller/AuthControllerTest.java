package com.dove.api.account.auth.controller;

import com.dove.api.TestApiApplication;
import com.dove.auth.application.service.CredentialService;
import com.dove.auth.application.service.InviteCodeService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.auth.domain.enums.MemberRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired CredentialService credentialService;
    @Autowired InviteCodeService inviteCodeService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MemberProfile profile = memberProfileCommandService.save(
                MemberProfile.create("t@t.com", "테스터", MemberRole.ADMIN));
        credentialService.save(
                Credential.create(profile.getId(), "testuser", passwordEncoder.encode("pass1234")));
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("로그인 성공 시 200")
        void shouldReturn200WhenLoginSuccess() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"testuser","password":"pass1234","rememberMe":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("비밀번호 틀리면 401")
        void shouldReturn401WhenPasswordWrong() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"testuser","password":"wrong","rememberMe":false}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("아이디 없으면 401")
        void shouldReturn401WhenUsernameNotFound() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"nobody","password":"pass1234","rememberMe":false}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("아이디에 대문자 포함 시 400")
        void shouldReturn400WhenUsernameHasUppercase() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"TestUser","password":"pass1234","rememberMe":false}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 refresh token 으로 새 토큰 발급")
        void shouldRefreshTokenWithValidRefreshTokenInBody() throws Exception {
            // 로그인 → refresh token 을 body 에서 추출
            var loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"testuser","password":"pass1234","rememberMe":false}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
            String refreshToken = loginJson.get("refreshToken").asText();

            // /auth/refresh 호출 → 새 access + 새 refresh
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("body 없으면 400")
        void shouldReturn400WhenRefreshWithoutBody() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("refresh token 이 유효하지 않으면 401")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"invalid.token.value\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("인증 없이 호출해도 204")
        void shouldReturn204OnLogoutWithoutAuth() throws Exception {
            // 인증 없이 호출해도 204 — 단순 멱등
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("유효한 초대 코드면 201")
        void shouldReturn201WhenRegisterWithValidCode() throws Exception {
            InviteCode code = inviteCodeService.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "testuser");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {"inviteCode":"%s","username":"newuser","password":"pass1234","email":"new@t.com","name":"신규회원"}
                                    """, code.getCode())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.username").value("newuser"));
        }

        @Test
        @DisplayName("초대 코드가 유효하지 않으면 400")
        void shouldReturn400WhenInviteCodeInvalid() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"inviteCode":"invalid","username":"newuser","password":"pass1234","email":"new@t.com","name":"신규회원"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("아이디가 이미 존재하면 409")
        void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
            InviteCode code = inviteCodeService.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "testuser");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {"inviteCode":"%s","username":"testuser","password":"pass1234","email":"other@t.com","name":"중복회원"}
                                    """, code.getCode())))
                    .andExpect(status().isConflict());
        }
    }
}
