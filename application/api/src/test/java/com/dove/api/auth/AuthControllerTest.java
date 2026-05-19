package com.dove.api.auth;

import com.dove.api.TestApiApplication;
import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.application.service.InviteCodeCommandService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.entity.InviteCode;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import com.dove.user.domain.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired CredentialCommandService credentialCommandService;
    @Autowired InviteCodeCommandService inviteCodeCommandService;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MemberProfile profile = memberProfileCommandService.save(
                MemberProfile.create("t@t.com", "테스터", MemberRole.ADMIN));
        credentialCommandService.save(
                Credential.create(profile.getId(), "testuser", passwordEncoder.encode("pass1234")));
    }

    @Test
    void shouldReturn200WhenLoginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"pass1234","rememberMe":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldReturn401WhenPasswordWrong() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"wrong","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUsernameNotFound() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nobody","password":"pass1234","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenUsernameHasUppercase() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"TestUser","password":"pass1234","rememberMe":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn201WhenRegisterWithValidCode() throws Exception {
        InviteCode code = inviteCodeCommandService.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "testuser");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"inviteCode":"%s","username":"newuser","password":"pass1234","email":"new@t.com","name":"신규회원"}
                                """, code.getCode())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void shouldReturn400WhenInviteCodeInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"invalid","username":"newuser","password":"pass1234","email":"new@t.com","name":"신규회원"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
        InviteCode code = inviteCodeCommandService.create(MemberRole.USER, LocalDateTime.now().plusDays(7), "testuser");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"inviteCode":"%s","username":"testuser","password":"pass1234","email":"other@t.com","name":"중복회원"}
                                """, code.getCode())))
                .andExpect(status().isConflict());
    }
}
