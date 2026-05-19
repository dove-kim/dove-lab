package com.dove.api.admin.invite;

import com.dove.api.TestApiApplication;
import com.dove.auth.application.service.CredentialCommandService;
import com.dove.auth.domain.entity.Credential;
import com.dove.security.JwtProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class InviteCodeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired CredentialCommandService credentialCommandService;
    @Autowired JwtProvider jwtProvider;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        MemberProfile admin = memberProfileCommandService.save(
                MemberProfile.create("a@a.com", "어드민", MemberRole.ROOT));
        credentialCommandService.save(
                Credential.create(admin.getId(), "admin", passwordEncoder.encode("pass")));

        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("u@u.com", "유저", MemberRole.USER));
        credentialCommandService.save(
                Credential.create(user.getId(), "user1", passwordEncoder.encode("pass")));

        adminToken = jwtProvider.generate(
                admin.getId(), "admin", admin.getName(), MemberRole.ROOT.name(), false, false);
        userToken = jwtProvider.generate(
                user.getId(), "user1", user.getName(), user.getRole().name(), false, false);
    }

    @Test
    void shouldReturn201WhenAdminCreatesInviteCode() throws Exception {
        mockMvc.perform(post("/root/invite-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldReturn403WhenUserTriesToCreateInviteCode() throws Exception {
        mockMvc.perform(post("/root/invite-codes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":7}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(post("/root/invite-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":7}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenRoleIsMissing() throws Exception {
        mockMvc.perform(post("/root/invite-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expireDays":7}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WhenAdminListsInviteCodes() throws Exception {
        mockMvc.perform(post("/root/invite-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":3}
                                """));

        mockMvc.perform(get("/root/invite-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").isNotEmpty());
    }

    @Test
    void shouldReturn403WhenUserListsInviteCodes() throws Exception {
        mockMvc.perform(get("/root/invite-codes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
