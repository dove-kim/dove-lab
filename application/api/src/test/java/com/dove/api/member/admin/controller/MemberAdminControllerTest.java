package com.dove.api.member.admin.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.entity.Credential;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class MemberAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired CredentialService credentialService;
    @Autowired PasswordEncoder passwordEncoder;

    private Long targetUserId;

    @BeforeEach
    void setUp() {
        MemberProfile user = memberProfileCommandService.save(
                MemberProfile.create("user@test.com", "일반유저", MemberRole.USER));
        targetUserId = user.getId();
        credentialService.save(
                Credential.create(targetUserId, "regularuser", passwordEncoder.encode("pass")));
    }

    @Nested
    @DisplayName("GET /admin/users")
    class ListMembers {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUserAccessesMemberList() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한이면 회원 목록 조회")
        void shouldReturnMemberListWhenAdmin() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{id}/role")
    class ChangeRole {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한이면 403")
        void shouldReturn403WhenAdminChangesRole() throws Exception {
            mockMvc.perform(patch("/admin/users/" + targetUserId + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"ADMIN"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 역할 변경 204")
        void shouldChangeRoleWhenRoot() throws Exception {
            mockMvc.perform(patch("/admin/users/" + targetUserId + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"ADMIN"}
                                    """))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("없는 사용자면 404")
        void shouldReturn404WhenChangingRoleForNonExistentUser() throws Exception {
            mockMvc.perform(patch("/admin/users/99999/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"ADMIN"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 유저 역할 변경 시도 시 403")
        void shouldReturn403WhenTryingToChangeRootRole() throws Exception {
            MemberProfile anotherRoot = memberProfileCommandService.save(
                    MemberProfile.create("anotherroot@test.com", "다른루트", MemberRole.ROOT));

            mockMvc.perform(patch("/admin/users/" + anotherRoot.getId() + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"role":"ADMIN"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /admin/users/{id}/reset-password")
    class ResetPassword {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한이면 403")
        void shouldReturn403WhenAdminResetsPassword() throws Exception {
            mockMvc.perform(post("/admin/users/" + targetUserId + "/reset-password"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 초기화 200 + 임시비밀번호 반환")
        void shouldResetPasswordWhenRoot() throws Exception {
            mockMvc.perform(post("/admin/users/" + targetUserId + "/reset-password"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("credential 없는 사용자면 404")
        void shouldReturn404WhenResetPasswordForUserWithoutCredential() throws Exception {
            MemberProfile noCredentialUser = memberProfileCommandService.save(
                    MemberProfile.create("nocred@test.com", "크레덴셜없는유저", MemberRole.USER));

            mockMvc.perform(post("/admin/users/" + noCredentialUser.getId() + "/reset-password"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/users/{id}")
    class DeleteUser {

        @Test
        @WithApiUser(role = "ADMIN")
        @DisplayName("ADMIN 권한이면 403")
        void shouldReturn403WhenAdminDeletesUser() throws Exception {
            mockMvc.perform(delete("/admin/users/" + targetUserId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 삭제 204 + 목록에 탈퇴일시 표시")
        void shouldSoftDeleteAndMarkDeletedInListWhenRoot() throws Exception {
            mockMvc.perform(delete("/admin/users/" + targetUserId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == " + targetUserId + ")].deletedAt").isNotEmpty());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("없는 사용자면 404")
        void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
            mockMvc.perform(delete("/admin/users/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 유저 삭제 시도 시 403")
        void shouldReturn403WhenDeletingRootUser() throws Exception {
            MemberProfile anotherRoot = memberProfileCommandService.save(
                    MemberProfile.create("rootdel@test.com", "다른루트", MemberRole.ROOT));

            mockMvc.perform(delete("/admin/users/" + anotherRoot.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
