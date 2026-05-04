package com.dove.stockapi.domain.admin;

import com.dove.member.application.service.MemberCommandService;
import com.dove.member.domain.entity.Member;
import com.dove.member.domain.entity.MemberRole;
import com.dove.stockapi.TestStockApiApplication;
import com.dove.stockapi.global.security.JwtProvider;
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

@SpringBootTest(classes = TestStockApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberCommandService memberCommandService;
    @Autowired JwtProvider jwtProvider;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        Member admin = memberCommandService.save(Member.create("admin", passwordEncoder.encode("pass"), "a@a.com", "어드민", MemberRole.ADMIN));
        Member user = memberCommandService.save(Member.create("user1", passwordEncoder.encode("pass"), "u@u.com", "유저", MemberRole.USER));

        adminToken = jwtProvider.generate(admin.getUsername(), admin.getName(), admin.getRole(), false);
        userToken = jwtProvider.generate(user.getUsername(), user.getName(), user.getRole(), false);
    }

    @Test
    void shouldReturn201WhenAdminCreatesInviteCode() throws Exception {
        mockMvc.perform(post("/api/admin/invite-codes")
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
        mockMvc.perform(post("/api/admin/invite-codes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":7}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(post("/api/admin/invite-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":7}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WhenAdminListsInviteCodes() throws Exception {
        mockMvc.perform(post("/api/admin/invite-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","expireDays":3}
                                """));

        mockMvc.perform(get("/api/admin/invite-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").isNotEmpty());
    }

    @Test
    void shouldReturn403WhenUserListsInviteCodes() throws Exception {
        mockMvc.perform(get("/api/admin/invite-codes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
