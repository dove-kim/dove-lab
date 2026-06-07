package com.dove.api.member.invite.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Nested
    @DisplayName("POST /admin/invite-codes")
    class CreateInviteCode {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 초대 코드 생성 201")
        void shouldReturn201WhenRootCreatesInviteCode() throws Exception {
            mockMvc.perform(post("/admin/invite-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"USER","expireDays":7}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").isNotEmpty())
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserTriesToCreateInviteCode() throws Exception {
            mockMvc.perform(post("/admin/invite-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"USER","expireDays":7}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenNoTokenProvided() throws Exception {
            mockMvc.perform(post("/admin/invite-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"USER","expireDays":7}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("role 누락 시 400")
        void shouldReturn400WhenRoleIsMissing() throws Exception {
            mockMvc.perform(post("/admin/invite-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"expireDays":7}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /admin/invite-codes")
    class ListInviteCodes {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 초대 코드 목록 200")
        void shouldReturn200WhenRootListsInviteCodes() throws Exception {
            mockMvc.perform(post("/admin/invite-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"USER","expireDays":3}
                                    """));

            mockMvc.perform(get("/admin/invite-codes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").isNotEmpty());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserListsInviteCodes() throws Exception {
            mockMvc.perform(get("/admin/invite-codes"))
                    .andExpect(status().isForbidden());
        }
    }
}
